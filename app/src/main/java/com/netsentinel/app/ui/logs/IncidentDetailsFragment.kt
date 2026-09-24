package com.netsentinel.app.ui.logs

import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.netsentinel.app.databinding.FragmentIncidentDetailsBinding
import com.netsentinel.app.repository.RoomIncidentRepository
import java.io.File

class IncidentDetailsFragment : Fragment() {

    private var _binding: FragmentIncidentDetailsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: IncidentViewModel by viewModels()
    private var photoUri: Uri? = null
    private var currentIncidentId: String = "INC-8942"

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && photoUri != null) {
            binding.imgEvidencePhoto.visibility = View.VISIBLE
            binding.imgEvidencePhoto.setImageURI(photoUri)
            binding.tvEvidencePlaceholder.visibility = View.GONE
            Toast.makeText(requireContext(), "Camera Evidence Attached to Incident $currentIncidentId", Toast.LENGTH_SHORT).show()

            // Update Room Repository
            context?.let { ctx ->
                RoomIncidentRepository(ctx).updateIncidentPhoto(currentIncidentId, photoUri.toString())
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIncidentDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.initRoomDatabase(requireContext())

        currentIncidentId = arguments?.getString("incidentId") ?: "INC-8942"
        val incident = viewModel.getIncidentById(currentIncidentId)

        incident?.let { item ->
            binding.tvDetailTitle.text = item.title
            binding.tvDetailSubtitle.text = "INCIDENT ID: ${item.id} | ${item.auditSessionId} (${item.timestampFormatted})"
            binding.tvDetailScore.text = "${item.threatScore} / 100 RISK"
            binding.tvDetailAiAnalysis.text = item.aiAnalysis

            val timelineFormatted = item.timeline.joinToString("\n") {
                "• [${it.time}] ${it.event} (${it.status})"
            }
            binding.tvDetailTimeline.text = timelineFormatted

            val specsFormatted = "Target BSSID: ${item.networkDetails.bssid}\n" +
                    "SSID: ${item.networkDetails.ssid}\n" +
                    "Vendor Signature: ${item.fingerprint.ouiVendor}\n" +
                    "Security Type: ${item.networkDetails.securityType}\n" +
                    "Gateway IP: ${item.networkDetails.gatewayIp}\n" +
                    "DNS Resolvers: ${item.networkDetails.dnsServers.joinToString(", ")}\n" +
                    "GPS Coordinates: ${item.gpsCoordinatesFormatted}"

            binding.tvDetailSpecs.text = specsFormatted

            if (item.evidencePhotoPlaceholder.startsWith("content://") || item.evidencePhotoPlaceholder.startsWith("file://")) {
                try {
                    binding.imgEvidencePhoto.visibility = View.VISIBLE
                    binding.imgEvidencePhoto.setImageURI(Uri.parse(item.evidencePhotoPlaceholder))
                    binding.tvEvidencePlaceholder.visibility = View.GONE
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        binding.btnCapturePhoto.setOnClickListener {
            launchCamera()
        }

        binding.btnExportReport.setOnClickListener {
            try {
                val reportsDir = File(requireContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "NetSentinel_Reports")
                if (!reportsDir.exists()) reportsDir.mkdirs()
                val reportFile = File(reportsDir, "INCIDENT_REPORT_${currentIncidentId}.txt")
                val content = buildString {
                    appendLine("==================================================")
                    appendLine("       NETSENTINEL ENTERPRISE FORENSIC REPORT     ")
                    appendLine("==================================================")
                    appendLine("Incident ID: $currentIncidentId")
                    appendLine("Generated: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}")
                    appendLine()
                    appendLine("--- INCIDENT OVERVIEW ---")
                    appendLine("Title: ${binding.tvDetailTitle.text}")
                    appendLine("Subtitle: ${binding.tvDetailSubtitle.text}")
                    appendLine("Threat Score: ${binding.tvDetailScore.text}")
                    appendLine()
                    appendLine("--- AI SECURITY ANALYSIS ---")
                    appendLine(binding.tvDetailAiAnalysis.text)
                    appendLine()
                    appendLine("--- NETWORK TELEMETRY & SPECS ---")
                    appendLine(binding.tvDetailSpecs.text)
                    appendLine()
                    appendLine("--- FORENSIC TIMELINE ---")
                    appendLine(binding.tvDetailTimeline.text)
                    appendLine()
                    appendLine("--- MULTIMEDIA EVIDENCE ---")
                    appendLine("Photo URI: ${photoUri ?: "No camera evidence captured"}")
                    appendLine("==================================================")
                }
                reportFile.writeText(content)
                Toast.makeText(requireContext(), "Forensic Report Saved: ${reportFile.name} (${reportFile.length()} bytes)", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Report generation failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun launchCamera() {
        try {
            val photoFile = File.createTempFile(
                "EVIDENCE_${currentIncidentId}_",
                ".jpg",
                requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            )
            photoUri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                photoFile
            )
            takePictureLauncher.launch(photoUri)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Opening Camera Intent...", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
