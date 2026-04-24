private fun register() {
    val username = binding.registerUsername.text.toString().trim()
    val email = binding.registerEmail.text.toString().trim()
    val password = binding.registerPassword.text.toString().trim()
    val repeat = binding.registerPasswordRepeat.text.toString().trim()

    if (username.isEmpty() || email.isEmpty() || password.isEmpty() || repeat.isEmpty()) {
        Toast.makeText(requireContext(), "Bitte alle Felder ausfüllen", Toast.LENGTH_SHORT).show()
        return
    }

    if (password != repeat) {
        Toast.makeText(requireContext(), "Passwörter stimmen nicht überein", Toast.LENGTH_SHORT).show()
        return
    }

    auth.createUserWithEmailAndPassword(email, password)
        .addOnSuccessListener {
            val uid = auth.currentUser?.uid ?: return@addOnSuccessListener

            val profile = UserProfile(
                username = username,
                email = email,
                rank_app = "Member",
                rank_ingame = "",
                uuid = uid,
                synced = false
            )

            db.collection("users")
                .document(uid)
                .set(profile)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Registrierung erfolgreich", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Profil konnte nicht gespeichert werden", Toast.LENGTH_SHORT).show()
                }
        }
        .addOnFailureListener {
            Toast.makeText(requireContext(), "Registrierung fehlgeschlagen", Toast.LENGTH_SHORT).show()
        }
}
