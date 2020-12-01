package com.benasque.quista.jr.riesgomsmho

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel: ViewModel() {


    private val _riesgo: MutableLiveData<String>? by lazy { MutableLiveData<String>() }
    fun getriesgo() = _riesgo as LiveData<String>
    fun setriesgo(i: String){
       _riesgo?.value = i
    }

    private val _recomendaciones: MutableLiveData<String>? by lazy { MutableLiveData<String>() }
    fun getrecomendaciones() = _recomendaciones as LiveData<String>
    fun setrecomendaciones(i: String){
       _recomendaciones?.value = i

    }




}

