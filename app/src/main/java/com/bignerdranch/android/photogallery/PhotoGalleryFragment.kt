package com.bignerdranch.android.photogallery

import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.media.Image
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

private const val TAG = "PhotoGalleryFragment"

 //25.8 исправление для листинга
 @Suppress("DEPRECATION")
 class PhotoGalleryFragment : Fragment() {

    private lateinit var photoGalleryViewModel: PhotoGalleryViewModel
    private lateinit var photoRecyclerView: RecyclerView
    private lateinit var thumbnailDownloader: ThumbnailDownloader<PhotoHolder>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        setHasOptionsMenu(true)
        photoGalleryViewModel = ViewModelProvider(this)[PhotoGalleryViewModel::class.java]
        val responseHandler = Handler()
        thumbnailDownloader = ThumbnailDownloader(responseHandler){
                photoHolder,bitmap ->
            val drawable=BitmapDrawable(resources,bitmap)

            photoHolder.imageView.setImageDrawable(drawable)
        }

        lifecycle.addObserver(thumbnailDownloader.fragmentLifecycleObserver)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewLifecycleOwner.lifecycle.addObserver(thumbnailDownloader.viewLifecycleObserver)
        val view = inflater.inflate(R.layout.fragment_photo_gallery, container, false)

        photoRecyclerView = view.findViewById(R.id.photo_recycler_view)
        photoRecyclerView.layoutManager = GridLayoutManager(context, 3) // количество столбцов



        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        photoGalleryViewModel.galleryItemLiveData.observe(
            viewLifecycleOwner
        ) { galleryItems ->
            Log.d(TAG, "Have gallery items from view model $galleryItems")
            photoRecyclerView.adapter = PhotoAdapter(galleryItems)
           // photoRecyclerView.adapter = PhotoText(galleryItems) //добавил в цикл текст

        }
    }



     override fun onDestroyView() {
         super.onDestroyView()
         viewLifecycleOwner.lifecycle.removeObserver(thumbnailDownloader.viewLifecycleObserver)

     }
     override fun onDestroy() {
         super.onDestroy()
         lifecycle.removeObserver(
             thumbnailDownloader.fragmentLifecycleObserver
         )
     }

     override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
         super.onCreateOptionsMenu(menu, inflater)
         inflater.inflate(R.menu.fragment_photo_gallery,menu)
         val searchItem: MenuItem = menu.findItem(R.id.menu_item_search)
         val searchView = searchItem.actionView as SearchView
         searchView.apply {
             setOnQueryTextListener(object : SearchView.OnQueryTextListener{
                 override fun onQueryTextSubmit(queryText: String): Boolean {
                     Log.d(TAG,"QueryTextSubmit:$queryText")
                     photoGalleryViewModel.fetchPhotos((queryText))
                     return true
                 }
                 override fun onQueryTextChange(queryText: String): Boolean {
                     Log.d(TAG,"QueryTextChange: $queryText")
                     return false
                 }
             })
             setOnSearchClickListener{
                 searchView.setQuery(photoGalleryViewModel.searchTerm,false)
             }
         }
     }

     override fun onOptionsItemSelected(item: MenuItem): Boolean {
         return when (item.itemId) {
             R.id.menu_item_clear -> {
                 photoGalleryViewModel.fetchPhotos("")
                 true
             }else -> super.onOptionsItemSelected(item)
         }
     }

     // вывод Адаптер картинок
    private class PhotoHolder(photoItemView: View)
         :RecyclerView.ViewHolder(photoItemView){
             val imageView: ImageView
             val titleView: TextView

             init{
                 imageView = photoItemView.findViewById(R.id.photo_image)
                 titleView = photoItemView.findViewById(R.id.photo_text)
             }


    //val bindDrawable:(Drawable) -> Unit = itemImageView::setImageDrawable
    }




     // Адаптер текста
//     private class PhotoHold(itemTextView: TextView) : RecyclerView.ViewHolder(itemTextView)
//     {
//         val bindTitle: (CharSequence) -> Unit = itemTextView::setText
//     }
//
//
//     private class PhotoText(private val galleryItems: List<GalleryItem>)
//         : RecyclerView.Adapter<PhotoHold>() {
//         override fun onCreateViewHolder(
//             parent: ViewGroup,
//             viewType: Int
//         ): PhotoHold {
//             val textView =
//                 TextView(parent.context)
//             return PhotoHold(textView)
//         }
//         override fun getItemCount(): Int = galleryItems.size
//         override fun onBindViewHolder(holde: PhotoHold, position: Int) {
//             val galleryItem =
//                 galleryItems[position]
//             holde.bindTitle(galleryItem.title)
//         }
//     }



//Вывод адаптер картинок



     private inner class PhotoAdapter(private val galleryItems: List<GalleryItem>)
        : RecyclerView.Adapter<PhotoHolder>() {

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): PhotoHolder {
            val view = layoutInflater.inflate(
                R.layout.list_item_gallery,
                parent,
                false
            )as ImageView
            return PhotoHolder(view)
        }

        override fun getItemCount(): Int = galleryItems.size

            //Вывод текст
// настройка отображения иконок


        override fun onBindViewHolder(holder: PhotoHolder, position: Int) {
            val galleryItem = galleryItems[position]

            val placeholder: Drawable = ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.jdun ) ?: ColorDrawable() // изменение самой иконки при загрузке

            holder.imageView.setImageDrawable(placeholder)
            holder.titleView.setText(galleryItem.title)


            //holder.bindDrawable(placeholder)
            thumbnailDownloader.queueThumbnail(holder,galleryItem.url)
        }
    }

    companion object {
        fun newInstance() = PhotoGalleryFragment()
    }


}