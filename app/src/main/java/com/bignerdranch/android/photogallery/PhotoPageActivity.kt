package com.bignerdranch.android.photogallery

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle

import androidx.appcompat.app.AppCompatActivity

class PhotoPageActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { // активность для своего веб браузера
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_page) // связывание со страницей

        val fm = supportFragmentManager
        val currentFragment = fm.findFragmentById(R.id.fragment_container)

        if(currentFragment == null) {
            val fragment = intent.data?.let { PhotoPageFragment.newInstance(it) }
            if (fragment != null) {
                fm.beginTransaction()
                    .add(R.id.fragment_container, fragment)
                    .commit()
            }
            }
        }





    companion object {
        fun newIntent(context: Context, photoPageUri: Uri) : Intent {
            return Intent(context, PhotoPageActivity::class.java).apply {
                data = photoPageUri
            }
        }
    }
}