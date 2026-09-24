package com.netsentinel.app.ui.logs

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.netsentinel.app.R
import com.netsentinel.app.data.model.Incident
import com.netsentinel.app.databinding.ItemIncidentCardBinding
import com.netsentinel.app.engine.ThreatSeverity

class IncidentAdapter(
    private val onItemClick: (Incident) -> Unit
) : ListAdapter<Incident, IncidentAdapter.IncidentViewHolder>(IncidentDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IncidentViewHolder {
        val binding = ItemIncidentCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return IncidentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: IncidentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class IncidentViewHolder(
        private val binding: ItemIncidentCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(incident: Incident) {
            binding.tvIncidentTitle.text = incident.title
            binding.tvIncidentTimestamp.text = "${incident.timestampFormatted} | ${incident.auditSessionId}"
            binding.tvThreatScore.text = incident.threatScore.toString()
            binding.tvSeverityBadge.text = incident.severity.name
            binding.tvStatusBadge.text = "Status: ${incident.status}"

            val severityColor = when (incident.severity) {
                ThreatSeverity.CRITICAL, ThreatSeverity.HIGH -> binding.root.context.getColor(R.color.alert_red)
                ThreatSeverity.MEDIUM -> binding.root.context.getColor(R.color.warning_yellow)
                else -> binding.root.context.getColor(R.color.neon_green)
            }
            binding.tvThreatScore.setTextColor(severityColor)
            binding.imgThumb.setColorFilter(severityColor)

            binding.root.setOnClickListener { onItemClick(incident) }
        }
    }

    class IncidentDiffCallback : DiffUtil.ItemCallback<Incident>() {
        override fun areItemsTheSame(oldItem: Incident, newItem: Incident): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Incident, newItem: Incident): Boolean = oldItem == newItem
    }
}
