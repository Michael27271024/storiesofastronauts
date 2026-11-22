package com.example.astronautstories.domain.model

enum class GameMode {
    STORY,
    ENDLESS,
    CHALLENGE,
    MULTIPLAYER
}

enum class GraphicsQuality {
    LOW,
    MEDIUM,
    HIGH
}

enum class WeaponType(
    val id: String,
    val titleFa: String,
    val titleEn: String,
    val titleAr: String,
    val damage: Int,
    val fireRateMs: Long,
    val projectileSpeed: Float,
    val spreadCount: Int,
    val colorHex: Long
) {
    STANDARD_BLASTER("blaster", "تفنگ لیزری استاندارد", "Standard Blaster", "مسدس الليزر القياسي", 1, 220L, 14f, 1, 0xFFFFEB3B),
    PLASMA_RIFLE("plasma", "تفنگ پلاسما", "Plasma Rifle", "بندقية البلازما", 2, 170L, 17f, 1, 0xFF00D9FF),
    LASER_SHOTGUN("shotgun", "شات‌گان لیزری سه‌تایی", "Tri-Laser Shotgun", "شوتجن ليزر ثلاثي", 2, 340L, 15f, 3, 0xFFFF6D00),
    PHOTON_SNIPER("sniper", "اسنایپر فوتونی", "Photon Sniper", "قناص الفوتون", 5, 420L, 24f, 1, 0xFFE040FB),
    GRAVITY_LAUNCHER("gravity", "پرتابگر موج گرانش", "Gravity Wave Cannon", "مدفع موجة الجاذبية", 4, 280L, 16f, 2, 0xFF00FFC3);

    fun localizedTitle(langCode: String): String = when (langCode) {
        "en" -> titleEn
        "ar" -> titleAr
        else -> titleFa
    }
}

enum class SkillType(
    val id: String,
    val titleFa: String,
    val titleEn: String,
    val titleAr: String,
    val descFa: String,
    val maxLevel: Int = 5
) {
    SPEED("speed", "چابکی رانشگر", "Thruster Agility", "رشاقة الدفع", "افزایش سرعت حرکت و پرش فضانورد"),
    FIREPOWER("firepower", "قدرت پلاسما", "Plasma Overcharge", "شحنة البلازما", "افزایش قدرت آسیب گلوله‌ها"),
    ARMOR("armor", "زره کیهانی", "Cosmic Armor", "الدرع الكوني", "افزایش حداکثر سلامت و مقاومت"),
    OXYGEN_CAP("oxygen", "کپسول اکسیژن", "Oxygen Reserve", "خزان الأكسجين", "افزایش زمان مأموریت و پایداری سپر"),
    LUCK("luck", "جذب کریستال", "Crystal Magnet", "مغناطيس البلورات", "افزایش پاداش کریستال و مهمات"),
    DRONE("drone", "پهپاد پشتیبان", "Support Drone", "طائرة الدعم", "تقویت سرعت و دقت هم‌تیمی‌ها و پهپاد");

    fun localizedTitle(langCode: String): String = when (langCode) {
        "en" -> titleEn
        "ar" -> titleAr
        else -> titleFa
    }
}

enum class HeroId(
    val id: String,
    val nameFa: String,
    val nameEn: String,
    val nameAr: String,
    val roleFa: String,
    val roleEn: String,
    val bonusSpeed: Float,
    val bonusHp: Int,
    val unlockCost: Int,
    val accentColor: Long
) {
    KOUROSH("kourosh", "کوروش", "Kourosh", "كورش", "فرمانده تیم و کاوشگر", "Squad Commander", 1.05f, 4, 0, 0xFF00D9FF),
    DARIUSH("dariush", "داریوش", "Dariush", "داريوش", "متخصص تسلیحات", "Weapons Specialist", 1.0f, 5, 0, 0xFFFFB300),
    SOHRAB("sohrab", "سهراب", "Sohrab", "سهراب", "مهندس پیشرانه و تعمیرات", "Propulsion Engineer", 1.15f, 4, 0, 0xFF10B981),
    ROSTAM("rostam", "رستم", "Rostam", "رستم", "تکاور خط مقدم", "Vanguard Trooper", 1.0f, 6, 0, 0xFFFF4D6D),
    ARASH("arash", "آرش", "Arash", "آرش", "تک‌تیرانداز مداری", "Orbital Marksman", 1.12f, 5, 350, 0xFFA78BFA),
    ATOUSA("atousa", "آتوسا", "Atousa", "أتوسا", "مهندس سپر انرژی", "Shield Architect", 1.10f, 6, 500, 0xFF38BDF8),
    KAVEH("kaveh", "کاوه", "Kaveh", "كاوة", "تخریب‌گر سنگین", "Heavy Breacher", 0.96f, 8, 700, 0xFFF97316),
    GORDAFARID("gordafarid", "گردآفرید", "Gordafarid", "غردافريد", "کماندوی جت‌پک", "Jetpack Commando", 1.25f, 5, 900, 0xFFF43F5E);

    fun localizedName(langCode: String): String = when (langCode) {
        "en" -> nameEn
        "ar" -> nameAr
        else -> nameFa
    }
}

enum class EnemyKind {
    ZOMBIE,
    ALIEN_SOLDIER,
    FLYER,
    SPLITTER,
    MINI_SPLITTER,
    ALIEN_SHIP,
    LASER_TURRET,
    BOSS_OVERLORD
}

enum class MissionObjective {
    OXYGEN_AND_PART,
    DEFEND_STATION,
    ALIEN_PURGE,
    RESCUE_HOSTAGES,
    STORY_EXPEDITION,
    BOSS_BATTLE
}

data class PlanetChapter(
    val chapterIndex: Int,
    val nameFa: String,
    val nameEn: String,
    val nameAr: String,
    val subtitleFa: String,
    val bgAssetPath: String,
    val groundColorHex: Long,
    val skyTopHex: Long,
    val skyBottomHex: Long,
    val accentHex: Long,
    val gravity: Float,
    val musicKey: String,
    val transitionVideoAsset: String
) {
    fun localizedName(langCode: String): String = when (langCode) {
        "en" -> nameEn
        "ar" -> nameAr
        else -> nameFa
    }
}

data class StageDefinition(
    val stageNumber: Int,
    val chapterIndex: Int,
    val titleFa: String,
    val titleEn: String,
    val briefingFa: String,
    val objective: MissionObjective,
    val targetBatteries: Int,
    val targetEnemies: Int,
    val timeLimitSeconds: Int,
    val worldWidth: Float,
    val hasDynamicDarkness: Boolean,
    val hasBetrayalChoice: Boolean,
    val hasBoss: Boolean,
    val enemyPool: List<EnemyKind>,
    val rewardXp: Int,
    val rewardCrystals: Int,
    val bgAssetPath: String,
    val musicKey: String,
    val transitionVideoAsset: String?
)

object GameCatalog {
    const val TOTAL_CHAPTERS = 10
    const val STAGES_PER_CHAPTER = 5
    const val TOTAL_STAGES = TOTAL_CHAPTERS * STAGES_PER_CHAPTER // 50
    const val MAX_STARS = TOTAL_STAGES * 3 // 150

    val chapters: List<PlanetChapter> = listOf(
        PlanetChapter(1, "ماه", "Moon", "القمر", "فصل ۱: سقوط و جمع‌آوری اکسیژن", "images/backgrounds/space_station.png", 0xFF616161, 0xFF0B1020, 0xFF1A2238, 0xFF00D9FF, 0.34f, "stage1", "videos/moon_to_mars.mp4"),
        PlanetChapter(2, "ایستگاه مداری", "Orbital Station", "المحطة المدارية", "فصل ۲: دفاع از پایگاه فضایی", "images/backgrounds/Inside_the_ship.jpg", 0xFF475569, 0xFF081226, 0xFF15294A, 0xFF38BDF8, 0.42f, "stage2", "videos/station_to_mars.mp4"),
        PlanetChapter(3, "مریخ", "Mars", "المريخ", "فصل ۳: سیاره سرخ و پایگاه ستاره‌زادگان", "images/backgrounds/mars_background.png", 0xFF9A3412, 0xFF280D09, 0xFF5C1D12, 0xFFF97316, 0.39f, "stage3", "videos/mars_to_saturn.mp4"),
        PlanetChapter(4, "زحل", "Saturn", "زحل", "فصل ۴: پژوهش علمی و برافراشتن پرچم", "images/backgrounds/saturn_background.png", 0xFF78350F, 0xFF1E140B, 0xFF452E15, 0xFFFFB300, 0.44f, "stage5", "videos/saturn_to_jupiter.mp4"),
        PlanetChapter(5, "مشتری", "Jupiter", "المشتري", "فصل ۵: توفان‌های مغناطیسی و کمین ناوگان", "images/backgrounds/jupiter_background.png", 0xFF7C2D12, 0xFF1E1024, 0xFF4C2233, 0xFFFB923C, 0.52f, "stage6", "videos/jupiter_to_mercury.mp4"),
        PlanetChapter(6, "عطارد", "Mercury", "عطارد", "فصل ۶: نورپردازی تاریک و دوراهی خیانت", "images/backgrounds/mercury_background.png", 0xFF1E293B, 0xFF030712, 0xFF0F2429, 0xFF00FFC3, 0.38f, "stage9", "videos/mercury_to_uranus.mp4"),
        PlanetChapter(7, "اورانوس", "Uranus", "أورانوس", "فصل ۷: میدان‌های یخی و پهپادهای تقسیم‌شونده", "images/backgrounds/space_station.png", 0xFF155E75, 0xFF061B26, 0xFF0E3B4F, 0xFF22D3EE, 0.40f, "stage7", "videos/uranus_to_neptune.mp4"),
        PlanetChapter(8, "نپتون", "Neptune", "نبتون", "فصل ۸: طوفان‌های آبی و آزادسازی گروگان‌ها", "images/backgrounds/alien_base_under_attack.jpg", 0xFF1E3A8A, 0xFF070F2B, 0xFF172B66, 0xFF60A5FA, 0.45f, "stage8", "videos/neptune_to_citadel.mp4"),
        PlanetChapter(9, "قلعه ستاره‌زادگان", "Starspawn Citadel", "قلعة النجوم", "فصل ۹: نفوذ به ناو فرماندهی بیگانگان", "images/backgrounds/rocket_interior.jpg", 0xFF4C1D95, 0xFF130726, 0xFF2E1065, 0xFFA78BFA, 0.46f, "boss", "videos/citadel_to_earth.mp4"),
        PlanetChapter(10, "بازگشت به زمین", "Return to Earth", "العودة إلى الأرض", "فصل ۱۰: نبرد نهایی و ورود به جو زمین", "images/backgrounds/iran.png", 0xFF065F46, 0xFF031418, 0xFF0B3B3C, 0xFF10B981, 0.45f, "boss", "videos/earth_return.mp4")
    )

    // 10 chapters * 5 stages = 50 distinct stages
    val allStages: List<StageDefinition> by lazy {
        val list = ArrayList<StageDefinition>(TOTAL_STAGES)
        for (chapterIdx in 1..TOTAL_CHAPTERS) {
            val chapter = chapters[chapterIdx - 1]
            for (sub in 1..STAGES_PER_CHAPTER) {
                val num = (chapterIdx - 1) * STAGES_PER_CHAPTER + sub
                val isBoss = sub == STAGES_PER_CHAPTER
                val isDark = (chapterIdx == 6 && sub >= 2) || (chapterIdx == 9 && sub in 2..4)
                val isBetrayalStage = (num == 29 || num == 30)
                val objective = when (sub) {
                    1 -> MissionObjective.OXYGEN_AND_PART
                    2 -> MissionObjective.DEFEND_STATION
                    3 -> MissionObjective.RESCUE_HOSTAGES
                    4 -> if (chapterIdx == 4) MissionObjective.STORY_EXPEDITION else MissionObjective.ALIEN_PURGE
                    else -> MissionObjective.BOSS_BATTLE
                }

                val enemies = when (chapterIdx) {
                    1 -> listOf(EnemyKind.ZOMBIE, EnemyKind.ALIEN_SOLDIER)
                    2 -> listOf(EnemyKind.ALIEN_SOLDIER, EnemyKind.FLYER)
                    3 -> listOf(EnemyKind.ALIEN_SOLDIER, EnemyKind.FLYER, EnemyKind.LASER_TURRET)
                    4 -> listOf(EnemyKind.ALIEN_SOLDIER, EnemyKind.SPLITTER)
                    5 -> listOf(EnemyKind.FLYER, EnemyKind.ALIEN_SHIP, EnemyKind.SPLITTER)
                    6 -> listOf(EnemyKind.ZOMBIE, EnemyKind.SPLITTER, EnemyKind.LASER_TURRET)
                    7 -> listOf(EnemyKind.FLYER, EnemyKind.SPLITTER, EnemyKind.ALIEN_SHIP)
                    8 -> listOf(EnemyKind.ALIEN_SOLDIER, EnemyKind.ALIEN_SHIP, EnemyKind.LASER_TURRET)
                    9 -> listOf(EnemyKind.SPLITTER, EnemyKind.ALIEN_SHIP, EnemyKind.LASER_TURRET)
                    else -> listOf(EnemyKind.ALIEN_SOLDIER, EnemyKind.FLYER, EnemyKind.SPLITTER, EnemyKind.ALIEN_SHIP)
                }

                val sectorLabelFa = when (sub) {
                    1 -> "بازیابی اکسیژن"
                    2 -> "دفاع از پایگاه"
                    3 -> "نجات هم‌تیمی‌ها"
                    4 -> if (chapterIdx == 4) "پژوهش و پرچم" else "پاک‌سازی منطقه"
                    else -> "باس ${chapter.nameFa}"
                }
                val sectorLabelEn = when (sub) {
                    1 -> "Oxygen"
                    2 -> "Defend"
                    3 -> "Rescue"
                    4 -> if (chapterIdx == 4) "Science" else "Purge"
                    else -> "Boss"
                }

                val briefing = when {
                    num == 1 -> "موشک آسیب دیده! ابتدا تفنگ را بردار، با شکار بیگانه‌ها ۲ باتری اکسیژن جمع کن و سپس قطعه گمشده موشک را بیاب."
                    isBetrayalStage -> "تاریکی عطارد ناگهانی است! با نور چراغ‌قوه پیش برو، XP کسب کن و در پایان مسیر تصمیم سرنوشت‌ساز خود را بگیر."
                    isBoss -> "فرمانده ناوگان بیگانگان در ${chapter.nameFa} ظاهر شده است! سپرها را بشکن و با شکست باس، ویدئوی انتقال به فصل بعد را باز کن."
                    objective == MissionObjective.OXYGEN_AND_PART -> "مأموریت ${chapter.nameFa} (مرحله $num): جمع‌آوری باتری اکسیژن و بازیابی قطعه سفینه پیش از پایان زمان."
                    objective == MissionObjective.RESCUE_HOSTAGES -> "مأموریت ${chapter.nameFa} (مرحله $num): پاک‌سازی نگهبانان و آزادسازی فضانوردان اسیر."
                    objective == MissionObjective.DEFEND_STATION -> "مأموریت ${chapter.nameFa} (مرحله $num): دفع موج هجوم دشمنان و حفاظت از سامانه دفاعی."
                    objective == MissionObjective.STORY_EXPEDITION -> "مأموریت ویژه زحل (مرحله $num): کاوش علمی حلقه‌ها و بازیابی قطعه ناوبری."
                    else -> "مأموریت ${chapter.nameFa} (مرحله $num): نابودی نیروهای پیشرو و پیشروی به سوی پایگاه بعدی."
                }

                list.add(
                    StageDefinition(
                        stageNumber = num,
                        chapterIndex = chapterIdx,
                        titleFa = if (isBoss) "باس ${chapter.nameFa}" else "${chapter.nameFa} • $sectorLabelFa",
                        titleEn = "${chapter.nameEn} • $sectorLabelEn",
                        briefingFa = briefing,
                        objective = objective,
                        targetBatteries = if (objective == MissionObjective.OXYGEN_AND_PART) 2 + (chapterIdx / 3) else 0,
                        targetEnemies = if (isBoss) 3 + (chapterIdx / 2) else 4 + sub * 2 + (chapterIdx * 2),
                        timeLimitSeconds = 180 + chapterIdx * 15,
                        worldWidth = 2700f + sub * 280f + chapterIdx * 50f,
                        hasDynamicDarkness = isDark,
                        hasBetrayalChoice = isBetrayalStage,
                        hasBoss = isBoss,
                        enemyPool = enemies,
                        rewardXp = 90 + num * 18 + if (isBoss) 250 else 0,
                        rewardCrystals = 45 + num * 8 + if (isBoss) 150 else 0,
                        bgAssetPath = chapter.bgAssetPath,
                        musicKey = if (isBoss) "boss" else chapter.musicKey,
                        transitionVideoAsset = if (sub == STAGES_PER_CHAPTER) chapter.transitionVideoAsset else null
                    )
                )
            }
        }
        list
    }

    fun getStage(stageNumber: Int): StageDefinition =
        allStages.getOrElse((stageNumber - 1).coerceIn(0, allStages.lastIndex)) { allStages.first() }
}
