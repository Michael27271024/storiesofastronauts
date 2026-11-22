package com.example.astronautstories.domain.model

enum class ShopCategory {
    WEAPONS,
    HEROES,
    BOOSTERS
}

data class ShopItemCatalog(
    val id: String,
    val category: ShopCategory,
    val titleFa: String,
    val titleEn: String,
    val titleAr: String,
    val descFa: String,
    val priceCrystals: Int,
    val weaponType: WeaponType? = null,
    val heroId: HeroId? = null
) {
    fun localizedTitle(langCode: String): String = when (langCode) {
        "en" -> titleEn
        "ar" -> titleAr
        else -> titleFa
    }
}

data class AchievementCatalog(
    val id: String,
    val titleFa: String,
    val titleEn: String,
    val descFa: String,
    val rewardCrystals: Int,
    val targetCount: Int
) {
    fun localizedTitle(langCode: String): String = if (langCode == "en") titleEn else titleFa
}

object StoreAndAchievements {
    val shopItems: List<ShopItemCatalog> = listOf(
        ShopItemCatalog("weapon_blaster", ShopCategory.WEAPONS, "تفنگ لیزری استاندارد", "Standard Blaster", "مسدس الليزر", "سلاح پایه فضانوردان با شلیک دقیق", 0, weaponType = WeaponType.STANDARD_BLASTER),
        ShopItemCatalog("weapon_plasma", ShopCategory.WEAPONS, "تفنگ پلاسما", "Plasma Rifle", "بندقية البلازما", "آسیب دوبرابر و سرعت شلیک بالا", 220, weaponType = WeaponType.PLASMA_RIFLE),
        ShopItemCatalog("weapon_shotgun", ShopCategory.WEAPONS, "شات‌گان لیزری سه‌تایی", "Tri-Laser Shotgun", "شوتجن ثلاثي", "شلیک ۳ پرتو هم‌زمان برای پاک‌سازی موج دشمنان", 420, weaponType = WeaponType.LASER_SHOTGUN),
        ShopItemCatalog("weapon_sniper", ShopCategory.WEAPONS, "اسنایپر فوتونی", "Photon Sniper", "قناص الفوتون", "شلیک پرقدرت ۵ واحدی برای شکار باس‌ها", 680, weaponType = WeaponType.PHOTON_SNIPER),
        ShopItemCatalog("weapon_gravity", ShopCategory.WEAPONS, "توپ موج گرانش", "Gravity Wave Cannon", "مدفع الجاذبية", "قوی‌ترین سلاح ناوگان با دو موج پلاسمای هم‌زمان", 950, weaponType = WeaponType.GRAVITY_LAUNCHER),

        ShopItemCatalog("hero_arash", ShopCategory.HEROES, "آرش (تک‌تیرانداز مداری)", "Arash (Marksman)", "آرش (القناص)", "سرعت بالا و دقت کشنده از راه دور", HeroId.ARASH.unlockCost, heroId = HeroId.ARASH),
        ShopItemCatalog("hero_atousa", ShopCategory.HEROES, "آتوسا (مهندس سپر)", "Atousa (Shield Tech)", "أتوسا (مهندسة الدروع)", "ظرفیت سپر انرژی بیشتر و تعادل عالی", HeroId.ATOUSA.unlockCost, heroId = HeroId.ATOUSA),
        ShopItemCatalog("hero_kaveh", ShopCategory.HEROES, "کاوه (تخریب‌گر سنگین)", "Kaveh (Breacher)", "كاوة (المقتحم)", "سلامت بسیار بالا برای نبردهای طولانی", HeroId.KAVEH.unlockCost, heroId = HeroId.KAVEH),
        ShopItemCatalog("hero_gordafarid", ShopCategory.HEROES, "گردآفرید (کماندوی جت‌پک)", "Gordafarid (Commando)", "غردافريد (كوماندوز)", "بیشترین سرعت حرکت و مانور فضایی", HeroId.GORDAFARID.unlockCost, heroId = HeroId.GORDAFARID),

        ShopItemCatalog("booster_medkit", ShopCategory.BOOSTERS, "بسته سلامت اضطراری (+۲ جان)", "Emergency Medkit", "حقيبة إسعافات", "در شروع مأموریت بعدی ۲ جان اضافه می‌افزاید", 90),
        ShopItemCatalog("booster_ammo", ShopCategory.BOOSTERS, "خشاب کوانتومی (+۴۰ تیر)", "Quantum Ammo Pack", "حزمة ذخيرة كمية", "در شروع مأموریت بعدی ۴۰ تیر اضافه دریافت کنید", 75),
        ShopItemCatalog("booster_shield", ShopCategory.BOOSTERS, "سپر پلاسمای موقت", "Plasma Overshield", "درع البلازما المؤقت", "شروع نبرد با سپر محافظ کامل", 120)
    )

    val achievements: List<AchievementCatalog> = listOf(
        AchievementCatalog("first_launch", "نخستین پرتاب", "First Liftoff", "اولین مأموریت خود را به پایان برسانید", 50, 1),
        AchievementCatalog("moon_survivor", "فاتح ماه", "Moon Conqueror", "تمامی ۵ مرحله فصل ماه را تکمیل کنید", 120, 5),
        AchievementCatalog("mars_raider", "شکارچی مریخ", "Mars Raider", "به مرحله ۱۵ و نبرد مریخ برسید", 180, 15),
        AchievementCatalog("saturn_scientist", "دانشمند زحل", "Saturn Scientist", "مأموریت علمی زحل را کامل کنید", 150, 20),
        AchievementCatalog("mercury_shadow", "شبح عطارد", "Mercury Shadow", "از دوراهی تاریک عطارد عبور کنید", 220, 30),
        AchievementCatalog("earth_hero", "قهرمان بازگشت به زمین", "Hero of Earth", "تمامی ۵۰ مرحله داستانی را فتح کنید", 1000, 50),
        AchievementCatalog("alien_hunter_25", "شکارچی تازه‌کار", "Rookie Hunter", "۲۵ موجود فضایی را شکست دهید", 80, 25),
        AchievementCatalog("alien_hunter_150", "فرمانده پاک‌ساز", "Fleet Exterminator", "۱۵۰ موجود فضایی را شکست دهید", 200, 150),
        AchievementCatalog("alien_hunter_500", "اسطوره کهکشان", "Galactic Legend", "۵۰۰ موجود فضایی را شکست دهید", 500, 500),
        AchievementCatalog("boss_slayer", "شکارچی باس‌ها", "Boss Slayer", "اولین باس فصل را شکست دهید", 140, 1),
        AchievementCatalog("endless_1000", "بقا در اعماق فضا", "Deep Space Survivor", "در حالت بی‌پایان به امتیاز ۱۰۰۰ برسید", 150, 1000),
        AchievementCatalog("endless_5000", "ناوگان شکست‌ناپذیر", "Unbreakable Fleet", "در حالت بی‌پایان به امتیاز ۵۰۰۰ برسید", 400, 5000),
        AchievementCatalog("challenge_winner", "قهرمان چالش روزانه", "Challenge Champion", "یک چالش روزانه زمان‌دار را ببرید", 180, 1),
        AchievementCatalog("armory_collector", "کلکسیونر سلاح‌ها", "Armory Collector", "حداقل ۳ سلاح جدید از فروشگاه بخرید", 200, 3),
        AchievementCatalog("skill_master", "استاد مهارت‌ها", "Skill Master", "درخت مهارت را ۵ بار ارتقا دهید", 160, 5),
        AchievementCatalog("squad_leader", "رهبر هشت‌گانه", "Squad Leader", "یک قهرمان جدید از فروشگاه آزاد کنید", 180, 1)
    )
}
