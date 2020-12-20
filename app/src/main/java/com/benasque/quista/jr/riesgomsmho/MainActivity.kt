package com.benasque.quista.jr.riesgomsmho

import android.annotation.SuppressLint
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.benasque.quista.jr.riesgomsmho.databinding.ActivityMainBinding
import net.yslibrary.android.keyboardvisibilityevent.util.UIUtil
import kotlin.math.*

class MainActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private val mViewModel: MainViewModel by lazy {
        ViewModelProvider(this).get(MainViewModel::class.java)
    }


    //<editor-fold desc="Menu">
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu items for use in the action bar
        val inflater = menuInflater
        inflater.inflate(R.menu.menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle presses on the action bar items
        if (item.itemId == R.id.action_settings) {
            Toast.makeText(this@MainActivity, R.string.AcercadeJosera, Toast.LENGTH_SHORT).apply {
                setGravity(Gravity.CENTER, 0, 0)
                view.setPadding(50, 50, 50, 50)
                view.setBackgroundColor(getColor(R.color.rojoPastel))
                show()
            }
        } else if (item.itemId == R.id.version) {
            var packageinfo: PackageInfo? = null
            try {
                packageinfo = packageManager.getPackageInfo(packageName, 0)
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
            }
            var version: String? = null
            if (packageinfo != null) {
                version = packageinfo.versionName
            }
            Toast.makeText(this@MainActivity, "Riesgo MSMH versión: $version", Toast.LENGTH_SHORT).apply {
                setGravity(Gravity.CENTER, 0, 0)
                view.setPadding(50, 50, 50, 50)
                view.setBackgroundColor(getColor(R.color.rojoPastel))
                show()
            }

        }
        return false
    }

    //</editor-fold>


    //</editor-fold>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        // Comprueba fecha límite
        if (CFecha.comprueba("01/01/2024")) {
            CFecha.alertaMSG(this)
        }

        // Muestra el teclado numérico
        UIUtil.showKeyboard(this, binding.editTextEdad)


        //</editor-fold>

        mViewModel.getriesgo().observe(this, {
            binding.textViewPorcentaje.text = it
        })

        mViewModel.getrecomendaciones().observe(this, {
            binding.textViewRecomendaciones.text = it
        })

        miCheckListener(binding.checkBoxMS)
        miCheckListener(binding.checkBoxSincope)
        miCheckListener(binding.checkBoxTV)

        //  Permite acceder al link
        binding.textGuiasEsc.movementMethod = LinkMovementMethod.getInstance()

        binding.buttonCalcular.setOnClickListener {
            when {
                binding.editTextEdad.text.isEmpty() || binding.editTextEdad.text.toString().toInt() <= 16 -> {
                    binding.scrollView.fullScroll(View.FOCUS_UP)
                    binding.editTextEdad.requestFocus()
                    if (binding.editTextEdad.text.isEmpty()) {
                        elToast(getString(R.string.strEdad_vacio))
                        UIUtil.showKeyboard(this, binding.editTextEdad)
                    } else {
                        elToast(getString(R.string.strMenores16))
                        binding.editTextEdad.selectAll()
                    }
                }
                binding.editTextGrosor.text.isEmpty() || binding.editTextGrosor.text.toString().toInt() == 0 ||
                        binding.editTextGrosor.text.toString().toInt() < 16 -> {
                    binding.scrollView.fullScroll(View.FOCUS_UP)
                    binding.editTextGrosor.requestFocus()

                    when {
                        binding.editTextGrosor.text.isEmpty() -> {
                            elToast(getString(R.string.strTabique_vacio))
                            UIUtil.showKeyboard(this, binding.editTextGrosor)
                        }
                        binding.editTextGrosor.text.toString().toInt() == 0 -> {
                            elToast(getString(R.string.strGrosor_mayor))
                        }
                        else -> {
                            elToast(getString(R.string.strTabique_fino))
                        }
                    }
                }

                binding.editTextAuricula.text.isEmpty() || binding.editTextAuricula.text.toString().toInt() == 0 -> {

                    binding.editTextAuricula.requestFocus()
                    binding.scrollView.fullScroll(View.FOCUS_UP)
                    elToast(getString(R.string.strTamano_auricula))
                    UIUtil.showKeyboard(this, binding.editTextAuricula)

                }
                else -> {
                    binding.scrollView.fullScroll(View.FOCUS_DOWN)
                    mViewModel.run {
                        setriesgo(misResultados(binding.editTextEdad.text.toString().toDouble(),
                                binding.editTextGrosor.text.toString().toDouble(),
                                binding.editTextAuricula.text.toString().toDouble(),
                                binding.editTextGradiente.text.toString().toDouble(),
                                binding.checkBoxMS.isChecked,
                                binding.checkBoxTV.isChecked,
                                binding.checkBoxSincope.isChecked))
                        setrecomendaciones(misRecomendaciones(binding.editTextEdad.text.toString().toDouble(),
                                binding.editTextGrosor.text.toString().toDouble(),
                                binding.editTextAuricula.text.toString().toDouble(),
                                binding.editTextGradiente.text.toString().toDouble(),
                                binding.checkBoxMS.isChecked,
                                binding.checkBoxTV.isChecked,
                                binding.checkBoxSincope.isChecked))
                    }
                    UIUtil.hideKeyboard(this)
                }
            }
        }
    }


    //<editor-fold desc="Todos los Calculos.">
    /*  Fórmula de cálculo de riesgo de muerte súbita a los 5 años de la ESC
                "O’Mahony C, Jichi F, Pavlou M, et al., Hypertrophic Cardiomyopathy
                Outcomes Investigators. A novel clinical risk prediction model for
                sudden cardiac death in hypertrophic cardiomyopathy (HCM risk-SCD),
                Eur Heart J, 2014;35:2010–20"
             */
    private fun micalculo(Tabique: Double, AI: Double, Gradiente: Double, edad: Double, historiaf: Boolean,
                          nsTV: Boolean, sincope: Boolean): Double {
        val t = Tabique * ccTabique1 - Tabique.pow(2.0) * ccTabique2
        val auricula = AI * ccAuricula
        val g = Gradiente * ccGradiente
        val h: Double = if (historiaf) {
            ccHisstoriaF
        } else {
            0.0
        }
        val tv: Double = if (nsTV) {
            ccTV
        } else {
            0.0
        }
        val s: Double = if (sincope) {
            ccSincope
        } else {
            0.0
        }
        val e = edad * ccEdad
        val suma = t + auricula + g + h + tv + s - e


        return try {
            val exponencial = exp(suma)
            (1 - ccExp.pow(exponencial)) * 100
        } catch (e: Exception) {
            0.0
        }
    }

    @SuppressLint("DefaultLocale")
    private fun misResultados(edad: Double, Tabique: Double, AI: Double, Gradiente: Double,
                              historiaf: Boolean, nsTV: Boolean, sincope: Boolean): String {

        return String.format("%.2f", micalculo(Tabique, AI, Gradiente, edad, historiaf, nsTV, sincope)) + "%"
    }

    private fun misRecomendaciones(edad: Double, Tabique: Double, AI: Double, Gradiente: Double,
                                   historiaf: Boolean, nsTV: Boolean, sincope: Boolean): String {
        return when {
            (micalculo(Tabique, AI, Gradiente, edad, historiaf, nsTV, sincope) < 4) -> getString(R.string.strDAI_no_indicado)
            (micalculo(Tabique, AI, Gradiente, edad, historiaf, nsTV, sincope) >= 4 && micalculo(Tabique, AI, Gradiente, edad, historiaf, nsTV, sincope) < 6) -> getString(R.string.strDAI_IIb)
            else -> getString(R.string.strDAI_IIa)
        }

    } //</editor-fold>

    companion object {
        //</editor-fold>
        //<editor-fold desc="Declaración de Cconstantes numéricas">
        private const val ccTabique1 = 0.15939858
        private const val ccTabique2 = 0.00294271
        private const val ccAuricula = 0.0259082
        private const val ccGradiente = 0.00446131
        private const val ccHisstoriaF = 0.4583082
        private const val ccTV = 0.82639195
        private const val ccSincope = 0.71650361
        private const val ccEdad = 0.01799934
        private const val ccExp = 0.998
    }

    private fun miCheckListener(ck: CheckBox) {
        ck.setOnClickListener {
            UIUtil.hideKeyboard(this)
        }
    }

    private fun elToast(miTexto: String) {
        Toast.makeText(applicationContext, miTexto, Toast.LENGTH_SHORT).apply {
            setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, 250)
            view.setPadding(50, 50, 50, 50)
            view.setBackgroundColor(getColor(R.color.verdeOscuro))
            show()
            
        }
    }
}