package com.netsentinel.app.ui.map

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.netsentinel.app.R
import com.netsentinel.app.databinding.FragmentThreatMapBinding
import com.netsentinel.app.location.GpsLocationCollector
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker

class ThreatMapFragment : Fragment() {

    private var _binding: FragmentThreatMapBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ThreatMapViewModel by viewModels()
    private lateinit var locationCollector: GpsLocationCollector
    private var currentLat = 37.7749
    private var currentLng = -122.4194

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Configuration.getInstance().load(
            requireContext(),
            requireContext().getSharedPreferences("osmdroid_prefs", Context.MODE_PRIVATE)
        )
        _binding = FragmentThreatMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        locationCollector = GpsLocationCollector(requireContext())

        setupDefaultTacticalCanvas()
        setupModeToggle()

        binding.fabCenterLocation.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                val (lat, lng) = locationCollector.getCurrentLocation()
                currentLat = lat
                currentLng = lng

                viewModel.updatePinsNearLocation(lat, lng)
                binding.cyberMapView.setLocationAndPins(lat, lng, viewModel.pins.value)

                val geoPoint = GeoPoint(lat, lng)
                binding.osmMapView.controller.animateTo(geoPoint)

                Toast.makeText(
                    requireContext(),
                    "GPS Location Centered: (${String.format("%.4f", lat)}°, ${String.format("%.4f", lng)}°)",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        binding.cyberMapView.onPinSelectedListener = { pin ->
            if (pin != null) {
                binding.cardNodeDetail.visibility = View.VISIBLE
                binding.tvSelectedNodeTitle.text = pin.title
                binding.tvSelectedNodeDetails.text = "${pin.details}\nCoordinates: (${String.format("%.4f", pin.latitude)}°, ${String.format("%.4f", pin.longitude)}°)"
            } else {
                binding.cardNodeDetail.visibility = View.GONE
            }
        }
    }

    private fun setupDefaultTacticalCanvas() {
        binding.cyberMapView.visibility = View.VISIBLE
        binding.osmMapView.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            val (lat, lng) = locationCollector.getCurrentLocation()
            currentLat = lat
            currentLng = lng

            viewModel.updatePinsNearLocation(lat, lng)
            binding.cyberMapView.setLocationAndPins(lat, lng, viewModel.pins.value)
        }
    }

    private fun setupOsmMapView() {
        binding.osmMapView.setTileSource(TileSourceFactory.MAPNIK)
        binding.osmMapView.setMultiTouchControls(true)
        binding.osmMapView.controller.setZoom(15.0)

        viewLifecycleOwner.lifecycleScope.launch {
            val userPoint = GeoPoint(currentLat, currentLng)
            binding.osmMapView.controller.setCenter(userPoint)

            binding.osmMapView.overlays.clear()

            val userMarker = Marker(binding.osmMapView)
            userMarker.position = userPoint
            userMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            userMarker.title = "My Device (Active Scanner)"
            binding.osmMapView.overlays.add(userMarker)

            val pins = viewModel.pins.value
            for (pin in pins) {
                val pMarker = Marker(binding.osmMapView)
                pMarker.position = GeoPoint(pin.latitude, pin.longitude)
                pMarker.title = pin.title
                pMarker.snippet = pin.details
                pMarker.setOnMarkerClickListener { m, _ ->
                    m.showInfoWindow()
                    binding.cardNodeDetail.visibility = View.VISIBLE
                    binding.tvSelectedNodeTitle.text = pin.title
                    binding.tvSelectedNodeDetails.text = pin.details
                    true
                }
                binding.osmMapView.overlays.add(pMarker)
            }
            binding.osmMapView.invalidate()
        }
    }

    private fun setupModeToggle() {
        binding.toggleGroupMapMode.check(R.id.btn_mode_cyber)
        binding.toggleGroupMapMode.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btn_mode_cyber -> {
                        binding.cyberMapView.visibility = View.VISIBLE
                        binding.osmMapView.visibility = View.GONE
                        binding.cyberMapView.setLocationAndPins(currentLat, currentLng, viewModel.pins.value)
                    }
                    R.id.btn_mode_osm -> {
                        binding.cyberMapView.visibility = View.GONE
                        binding.osmMapView.visibility = View.VISIBLE
                        setupOsmMapView()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        _binding?.osmMapView?.onResume()
    }

    override fun onPause() {
        super.onPause()
        _binding?.osmMapView?.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding?.osmMapView?.onDetach()
        _binding = null
    }
}
