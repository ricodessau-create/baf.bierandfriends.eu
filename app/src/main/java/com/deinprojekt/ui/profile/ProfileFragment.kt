private fun loadUserProfile() {
    lifecycleScope.launch {
        val profile = userRepository.getUserProfile()

        if (profile != null) {
            binding.profileUsername.text = profile.username
            binding.profileEmail.text = profile.email
            binding.profileRank.text = "App‑Rang: ${profile.rank_app}"
            binding.profileIngameRank.text = "Ingame‑Rang: ${profile.rank_ingame.ifEmpty { "Nicht synchronisiert" }}"
        } else {
            binding.profileUsername.text = "Unbekannt"
            binding.profileEmail.text = "Keine Daten gefunden"
            binding.profileRank.text = ""
            binding.profileIngameRank.text = ""
        }
    }
}
