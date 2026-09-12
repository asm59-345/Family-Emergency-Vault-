package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Curated typography themes designed specifically for Family Emergency Vault.
 */
enum class VaultFontTheme(
    val id: String,
    val titleEn: String,
    val titleHi: String,
    val descriptionEn: String,
    val descriptionHi: String,
    val headingFamily: FontFamily,
    val bodyFamily: FontFamily,
    val chipLabel: String
) {
    EXECUTIVE(
        id = "executive",
        titleEn = "Executive Banking",
        titleHi = "एग्जीक्यूटिव बैंकिंग",
        descriptionEn = "Crisp, Swiss-banking geometric Sans-Serif with sharp contrast and structured legibility for financial assets.",
        descriptionHi = "स्विस बैंकिंग शैली: स्पष्ट, आधुनिक और सुरक्षित वित्तीय डेटा के लिए सर्वोत्तम।",
        headingFamily = FontFamily.SansSerif,
        bodyFamily = FontFamily.SansSerif,
        chipLabel = "🏛️ Executive"
    ),
    HERITAGE(
        id = "heritage",
        titleEn = "Heritage Trust & Estate",
        titleHi = "हेरिटेज ट्रस्ट और वसीयत",
        descriptionEn = "Distinguished legal Serif headings paired with clean body text, evocative of formal deeds and estate testaments.",
        descriptionHi = "पारंपरिक कानूनी दस्तावेज और वसीयत शैली: प्रतिष्ठित सेरिफ़ फोंट।",
        headingFamily = FontFamily.Serif,
        bodyFamily = FontFamily.SansSerif,
        chipLabel = "📜 Heritage"
    ),
    CYBER_DEFENSE(
        id = "cyber",
        titleEn = "High-Tech Cyber Vault",
        titleHi = "साइबर डिफेंस वॉल्ट",
        descriptionEn = "Technical Monospace titles and cryptographic tracking for zero-knowledge vault integrity.",
        descriptionHi = "तकनीकी मोनोस्पेस शैली: एन्क्रिप्टेड सुरक्षा और सुरक्षा प्रोटोकॉल।",
        headingFamily = FontFamily.Monospace,
        bodyFamily = FontFamily.SansSerif,
        chipLabel = "🔒 Cyber Vault"
    ),
    HUMANIST_CARE(
        id = "humanist",
        titleEn = "Humanist Care & Comfort",
        titleHi = "ह्यूमनिस्ट केयर और सहजता",
        descriptionEn = "Gentle, accessible typography with open letter-spacing and generous line height for stress-free crisis reading.",
        descriptionHi = "सरल और सहज फोंट: आपातकालीन स्थिति में सभी आयु वर्ग के लिए पढ़ने में आसान।",
        headingFamily = FontFamily.SansSerif,
        bodyFamily = FontFamily.SansSerif,
        chipLabel = "🌿 Humanist"
    )
}

fun getTypographyForTheme(theme: VaultFontTheme): Typography {
    val hFamily = theme.headingFamily
    val bFamily = theme.bodyFamily

    return when (theme) {
        VaultFontTheme.EXECUTIVE -> Typography(
            displayLarge = TextStyle(
                fontFamily = hFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 52.sp,
                lineHeight = 60.sp,
                letterSpacing = (-0.75).sp
            ),
            displayMedium = TextStyle(
                fontFamily = hFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 42.sp,
                lineHeight = 48.sp,
                letterSpacing = (-0.5).sp
            ),
            displaySmall = TextStyle(
                fontFamily = hFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 32.sp,
                lineHeight = 40.sp,
                letterSpacing = (-0.25).sp
            ),
            headlineLarge = TextStyle(
                fontFamily = hFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                lineHeight = 36.sp,
                letterSpacing = (-0.2).sp
            ),
            headlineMedium = TextStyle(
                fontFamily = hFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 24.sp,
                lineHeight = 32.sp,
                letterSpacing = 0.sp
            ),
            headlineSmall = TextStyle(
                fontFamily = hFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                lineHeight = 28.sp,
                letterSpacing = 0.sp
            ),
            titleLarge = TextStyle(
                fontFamily = hFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                lineHeight = 26.sp,
                letterSpacing = 0.sp
            ),
            titleMedium = TextStyle(
                fontFamily = hFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.1.sp
            ),
            titleSmall = TextStyle(
                fontFamily = hFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.1.sp
            ),
            bodyLarge = TextStyle(
                fontFamily = bFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                letterSpacing = 0.2.sp
            ),
            bodyMedium = TextStyle(
                fontFamily = bFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                letterSpacing = 0.2.sp
            ),
            bodySmall = TextStyle(
                fontFamily = bFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.3.sp
            ),
            labelLarge = TextStyle(
                fontFamily = hFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                letterSpacing = 0.2.sp
            ),
            labelMedium = TextStyle(
                fontFamily = bFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                letterSpacing = 0.4.sp
            ),
            labelSmall = TextStyle(
                fontFamily = bFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 10.sp,
                lineHeight = 14.sp,
                letterSpacing = 0.5.sp
            )
        )

        VaultFontTheme.HERITAGE -> Typography(
            displayLarge = TextStyle(
                fontFamily = hFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 50.sp,
                lineHeight = 58.sp,
                letterSpacing = 0.sp
            ),
            displayMedium = TextStyle(
                fontFamily = hFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 40.sp,
                lineHeight = 48.sp,
                letterSpacing = 0.sp
            ),
            displaySmall = TextStyle(
                fontFamily = hFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 32.sp,
                lineHeight = 40.sp,
                letterSpacing = 0.sp
            ),
            headlineLarge = TextStyle(
                fontFamily = hFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                lineHeight = 36.sp,
                letterSpacing = 0.sp
            ),
            headlineMedium = TextStyle(
                fontFamily = hFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                lineHeight = 32.sp,
                letterSpacing = 0.15.sp
            ),
            headlineSmall = TextStyle(
                fontFamily = hFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 20.sp,
                lineHeight = 28.sp,
                letterSpacing = 0.sp
            ),
            titleLarge = TextStyle(
                fontFamily = hFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 21.sp,
                lineHeight = 28.sp,
                letterSpacing = 0.15.sp
            ),
            titleMedium = TextStyle(
                fontFamily = hFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.1.sp
            ),
            titleSmall = TextStyle(
                fontFamily = hFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.1.sp
            ),
            bodyLarge = TextStyle(
                fontFamily = bFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp,
                lineHeight = 23.sp,
                letterSpacing = 0.3.sp
            ),
            bodyMedium = TextStyle(
                fontFamily = bFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                letterSpacing = 0.25.sp
            ),
            bodySmall = TextStyle(
                fontFamily = bFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.4.sp
            ),
            labelLarge = TextStyle(
                fontFamily = hFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                letterSpacing = 0.2.sp
            ),
            labelMedium = TextStyle(
                fontFamily = bFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                letterSpacing = 0.4.sp
            ),
            labelSmall = TextStyle(
                fontFamily = bFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 10.sp,
                lineHeight = 14.sp,
                letterSpacing = 0.5.sp
            )
        )

        VaultFontTheme.CYBER_DEFENSE -> Typography(
            displayLarge = TextStyle(
                fontFamily = hFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 48.sp,
                lineHeight = 56.sp,
                letterSpacing = (-0.5).sp
            ),
            displayMedium = TextStyle(
                fontFamily = hFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 38.sp,
                lineHeight = 44.sp,
                letterSpacing = 0.sp
            ),
            displaySmall = TextStyle(
                fontFamily = hFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 30.sp,
                lineHeight = 36.sp,
                letterSpacing = 0.sp
            ),
            headlineLarge = TextStyle(
                fontFamily = hFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
                lineHeight = 32.sp,
                letterSpacing = 0.sp
            ),
            headlineMedium = TextStyle(
                fontFamily = hFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
                lineHeight = 28.sp,
                letterSpacing = 0.sp
            ),
            headlineSmall = TextStyle(
                fontFamily = hFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 18.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.sp
            ),
            titleLarge = TextStyle(
                fontFamily = hFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.sp
            ),
            titleMedium = TextStyle(
                fontFamily = hFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                letterSpacing = 0.1.sp
            ),
            titleSmall = TextStyle(
                fontFamily = hFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                letterSpacing = 0.1.sp
            ),
            bodyLarge = TextStyle(
                fontFamily = bFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                letterSpacing = 0.3.sp
            ),
            bodyMedium = TextStyle(
                fontFamily = bFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                letterSpacing = 0.25.sp
            ),
            bodySmall = TextStyle(
                fontFamily = bFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                letterSpacing = 0.3.sp
            ),
            labelLarge = TextStyle(
                fontFamily = hFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.5.sp
            ),
            labelMedium = TextStyle(
                fontFamily = hFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                letterSpacing = 0.5.sp
            ),
            labelSmall = TextStyle(
                fontFamily = hFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 10.sp,
                lineHeight = 14.sp,
                letterSpacing = 0.6.sp
            )
        )

        VaultFontTheme.HUMANIST_CARE -> Typography(
            displayLarge = TextStyle(
                fontFamily = hFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 52.sp,
                lineHeight = 62.sp,
                letterSpacing = 0.sp
            ),
            displayMedium = TextStyle(
                fontFamily = hFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 42.sp,
                lineHeight = 50.sp,
                letterSpacing = 0.sp
            ),
            displaySmall = TextStyle(
                fontFamily = hFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 32.sp,
                lineHeight = 40.sp,
                letterSpacing = 0.2.sp
            ),
            headlineLarge = TextStyle(
                fontFamily = hFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 28.sp,
                lineHeight = 36.sp,
                letterSpacing = 0.15.sp
            ),
            headlineMedium = TextStyle(
                fontFamily = hFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 24.sp,
                lineHeight = 32.sp,
                letterSpacing = 0.15.sp
            ),
            headlineSmall = TextStyle(
                fontFamily = hFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 20.sp,
                lineHeight = 28.sp,
                letterSpacing = 0.1.sp
            ),
            titleLarge = TextStyle(
                fontFamily = hFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                lineHeight = 28.sp,
                letterSpacing = 0.2.sp
            ),
            titleMedium = TextStyle(
                fontFamily = hFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.2.sp
            ),
            titleSmall = TextStyle(
                fontFamily = hFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.15.sp
            ),
            bodyLarge = TextStyle(
                fontFamily = bFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.4.sp
            ),
            bodyMedium = TextStyle(
                fontFamily = bFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                letterSpacing = 0.3.sp
            ),
            bodySmall = TextStyle(
                fontFamily = bFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                letterSpacing = 0.4.sp
            ),
            labelLarge = TextStyle(
                fontFamily = hFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                letterSpacing = 0.25.sp
            ),
            labelMedium = TextStyle(
                fontFamily = bFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.4.sp
            ),
            labelSmall = TextStyle(
                fontFamily = bFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 10.sp,
                lineHeight = 14.sp,
                letterSpacing = 0.5.sp
            )
        )
    }
}

val Typography = getTypographyForTheme(VaultFontTheme.EXECUTIVE)

