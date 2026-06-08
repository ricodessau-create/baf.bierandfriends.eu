override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        // Deep-Link: baf://app/delete-account
        if (intent.data?.toString() == "baf://app/delete-account") {
            Handler(Looper.getMainLooper()).postDelayed({
                try { findNavController(R.id.nav_host_fragment).navigate(R.id.profileFragment) }
                catch (_: Exception) {}
            }, 300)
            return
        }

        val type = getNotificationType(intent) ?: return
        Handler(Looper.getMainLooper()).postDelayed({
            handleNotificationNavigation(type)
        }, 300)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        FirebaseApp.initializeApp(this)

        requestNotificationPermission()

        val navController = findNavController(R.id.nav_host_fragment)
        binding.bottomNavigation.setupWithNavController(navController)

        val noBottomNav = setOf(
            R.id.loginFragment, R.id.registerFragment, R.id.profileFragment,
            R.id.newPostFragment, R.id.newTicketFragment, R.id.marketCreateFragment,
            R.id.marketDetailFragment, R.id.chatFragment, R.id.privateChatFragment,
            R.id.postDetailFragment, R.id.userProfileFragment, R.id.ticketDetailFragment
        )

        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.bottomNavigation.visibility =
                if (destination.id in noBottomNav) View.GONE else View.VISIBLE
        }

        // Deep-Link beim Kaltstart
        if (intent?.data?.toString() == "baf://app/delete-account") {
            Handler(Looper.getMainLooper()).postDelayed({
                try { findNavController(R.id.nav_host_fragment).navigate(R.id.profileFragment) }
                catch (_: Exception) {}
            }, 1000)
            return
        }

        val type = getNotificationType(intent)
        if (type != null) {
            Handler(Looper.getMainLooper()).postDelayed({
                handleNotificationNavigation(type)
            }, 1000)
        }
    }
