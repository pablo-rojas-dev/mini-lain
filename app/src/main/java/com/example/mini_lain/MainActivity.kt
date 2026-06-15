package com.example.mini_lain

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.mini_lain.databinding.ActivityMainBinding
import com.example.mini_lain.LainFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fcvContenedorFragmento, LainFragment())
                .commit()
        }
    }
}