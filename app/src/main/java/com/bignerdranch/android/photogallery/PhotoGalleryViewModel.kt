package com.bignerdranch.android.photogallery

import android.app.Application
import androidx.lifecycle.*
import com.bignerdranch.android.photogallery.api.FlickrFetchr

class PhotoGalleryViewModel(private val app: Application) : AndroidViewModel(app) {

    val galleryItemLiveData: LiveData<List<GalleryItem>>

    private val flickrFetchr = FlickrFetchr()
    private val mutableSearchTerm = MutableLiveData<String>() // хранение последнего запроса


    init {

        mutableSearchTerm.value = QueryPreference.getStoredQuery(app) //запрос поисковой выдачи
        galleryItemLiveData = Transformations.switchMap(mutableSearchTerm) {searchTerm ->
            if(searchTerm.isBlank()) {
                flickrFetchr.fetchPhotos() // если пустой запрос то показывабт лучшие фото
            } else {
                flickrFetchr.searchPhotos(searchTerm)
            }

            // поиск фото
    }}

    fun fetchPhotos(query: String = "") {
        QueryPreference.setStoredQuery(app, query)
        mutableSearchTerm.value = query

    }
}