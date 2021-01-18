package com.bignerdranch.android.photogallery

import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.*
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainer
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.*
import com.bignerdranch.android.photogallery.api.FlickrApi
import com.bignerdranch.android.photogallery.api.FlickrFetchr
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit

private const val TAG = "PhotoGalleryFragment"
private const val POLL_WORK = "POLL_WORK"


class PhotoGalleryFragment: VisibleFragment() {

    private lateinit var photoGalleryViewModel: PhotoGalleryViewModel
    private lateinit var photoRecyclerView: RecyclerView
    private lateinit var thumbnailDownloader: ThumbnailDownloader<PhotoHolder>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        photoGalleryViewModel = ViewModelProviders.of(this).get(PhotoGalleryViewModel::class.java)
        retainInstance = true
        setHasOptionsMenu(true) // опции меню
        val responseHandler = Handler() // присоединяем к главному потоку
        thumbnailDownloader =
            ThumbnailDownloader(responseHandler) {photoHolder, bitmap ->
                val drawable = BitmapDrawable(resources, bitmap)
                photoHolder.bindDrawable(drawable) // обработка изображения после получения
            }
        lifecycle.addObserver(thumbnailDownloader.fragmentLifecycleObserver) // добавляем второго наблюдатедя


    }
// 529

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewLifecycleOwner.lifecycle.addObserver(
            thumbnailDownloader.viewLifecycleObserver // второй наблюдатель во фрагменте
        )

        val view = inflater.inflate(R.layout.fragment_photo_gallery, container, false )
        photoRecyclerView = view.findViewById(R.id.photo_recycler_view)
        photoRecyclerView.layoutManager = GridLayoutManager(context, 3)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        photoGalleryViewModel.galleryItemLiveData.observe(
            viewLifecycleOwner,
            Observer {galleryItems ->
                Log.d(TAG, "Have gallery items from view model $galleryItems")
                photoRecyclerView.adapter = PhotoAdapter(galleryItems)
            }
        )
    }

    override fun onDestroyView() { // отмена регистрации наблюдателя
        super.onDestroyView()
        viewLifecycleOwner.lifecycle.removeObserver(
            thumbnailDownloader.viewLifecycleObserver
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycle.removeObserver(
            thumbnailDownloader.fragmentLifecycleObserver
        )
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_photo_gallery, menu) // добавляем меню

        val searchItem: MenuItem = menu.findItem(R.id.menu_item_search) // поиск через меню
        val searchView = searchItem.actionView as SearchView // поисковый запрос

        searchView.apply {

            setOnQueryTextListener(object : SearchView.OnQueryTextListener { // слушатель событий то что было до
                override fun onQueryTextSubmit(queryText: String) : Boolean {
                Log.d(TAG, "QueryTextSubmit: $queryText")
                photoGalleryViewModel.fetchPhotos(queryText)
                return true
            }

                override fun onQueryTextChange(queryText: String?): Boolean { // изменение текста
                    Log.d(TAG, "QueryTextChange: $queryText")
                    return false
                }
            })
            setOnSearchClickListener {
                val query = QueryPreference.getStoredQuery(requireContext())
                searchView.setQuery(query, false) // предварительное заполнение
            }
        }

        val toggleItem = menu.findItem(R.id.menu_item_toggle_polling) // привязываем опрос
        val isPolling = QueryPreference.isPolling(requireContext())
        val toggleItemTitle = if(isPolling) {
            R.string.stop_polling
        } else {
            R.string.start_polling
        }
        toggleItem.setTitle(toggleItemTitle)

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_item_clear -> { // очистить поисковый запрос
                photoGalleryViewModel.fetchPhotos("")
                true
            }
             R.id.menu_item_toggle_polling -> {
                 val isPolling = QueryPreference.isPolling(requireContext()) // обрабатываем клики
                 if(isPolling) {
                     WorkManager.getInstance().cancelUniqueWork(POLL_WORK)
                     QueryPreference.setPolling(requireContext(), false) // если работник
                     // выключен, включаем нового
                 } else {
                     val constraints = Constraints.Builder()
                             .setRequiredNetworkType(NetworkType.UNMETERED) // проверка
                             // подключения к интернету
                             .build()

                     val periodicRequest = PeriodicWorkRequest
                             .Builder(PollWorker::class.java, 15,TimeUnit.MINUTES ) // минимально
                             // возможный интервал работы
                             .setConstraints(constraints)
                             .build()
                     WorkManager.getInstance().enqueueUniquePeriodicWork(POLL_WORK,
                            ExistingPeriodicWorkPolicy.KEEP, // сохраняем уже существующий
                             // запрос, а не создаем другой
                            periodicRequest)
                     QueryPreference.setPolling(requireContext(), true)

                 }
                 activity?.invalidateOptionsMenu()
                 return true
             }
            else -> super.onOptionsItemSelected(item)
        }

        // 628
    }

    private inner class PhotoHolder(private val itemImageView: ImageView)
        :RecyclerView.ViewHolder(itemImageView), View.OnClickListener
    {

        private lateinit var galleryItem: GalleryItem

        init {
            itemView.setOnClickListener(this) // неявный интент при нажатии
        }


        val bindDrawable: (Drawable) -> Unit  = itemImageView::setImageDrawable

        fun bindGalleryItem(item: GalleryItem) {
            galleryItem = item
        }

        override fun onClick(view: View) { // обработка нажатия
            val intent = PhotoPageActivity.newIntent(requireContext(), galleryItem.photoPageUri) // открытие браузера
            startActivity(intent)
        }

    }


    private inner class PhotoAdapter(private val galleryItems: List<GalleryItem>)
        : RecyclerView.Adapter<PhotoHolder>()  {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoHolder {
            val view = layoutInflater.inflate(
                R.layout.list_item_gallery,
                parent,
                false
            ) as ImageView
            return PhotoHolder(view)
        }

        override fun getItemCount(): Int = galleryItems.size

        override fun onBindViewHolder(holder: PhotoHolder, position: Int) {
            val galleryItem = galleryItems[position]
            holder.bindGalleryItem(galleryItem) // связывание с айтемом
            val placeholder: Drawable = ContextCompat.getDrawable(
                                            requireContext(),
                                            R.drawable.noth
            ) ?: ColorDrawable()
                holder.bindDrawable(placeholder)

            thumbnailDownloader.queueThumbnail(holder, galleryItem.url)
                // 573
        }

    }

    companion object {
        fun newInstance() = PhotoGalleryFragment()
    }
}