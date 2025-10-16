package com.example.a2048

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView


class ScoreAdapter(private val scores: List<ScoreItem>) :
    RecyclerView.Adapter<ScoreAdapter.ScoreViewHolder>() {

    class ScoreViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvPlace: TextView = view.findViewById(R.id.tvPlace)
        val tvScore: TextView = view.findViewById(R.id.tvScore)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScoreViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_score_row, parent, false)
        return ScoreViewHolder(view)
    }

    override fun onBindViewHolder(holder: ScoreViewHolder, position: Int) {
        val item = scores[position]
        holder.tvPlace.text = "${item.place}."
        holder.tvScore.text = item.score.toString()
    }

    override fun getItemCount(): Int = scores.size
}
