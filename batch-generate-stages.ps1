/**
 * Batch Stage Generator - Creates stages 4-10
 * Run this in PowerShell to generate all remaining stages
 */

$stagesData = @{
    4 = @{ name = 'اریس'; story = '⚡ کهکشان اریس!\n\nدشمنان تاریک منتظر هستند.\nتمام دشمنان را شکست بده!' }
    5 = @{ name = 'ستاره سوار'; story = '⭐ در میان ستارگان!\n\nآخرین مستعمره در خطر!\nتمام فضانوردان را شکست بده!' }
    6 = @{ name = 'فناتوس'; story = '🌙 سیاره مرموز!\n\nتاریک‌ترین سیاره‌ای که دیده‌ام.\nدشمنان قوی‌تر منتظر هستند!' }
    8 = @{ name = 'پایگاه تاریک'; story = '🌑 تاریکی مطلق!\n\nپایگاه سری دشمن کشف شد.\nتمام دفاعات را از بین بر!' }
    9 = @{ name = 'دوستی ابدی'; story = '💫 پایانی نزدیک!\n\nدرخت‌های دوستی حتی در جنگ برقرار است.\nآخرین دشمنان را شکست بده!' }
    10 = @{ name = 'بازگشت به خانه'; story = '🏠 بازگشت!\n\nآخرین مرحله سفر.\nتمام دشمنان را شکست بده و خانه برگرد!' }
}

$templatePath = ".\stage-generator-template.txt"

foreach ($stageNum in @(4, 5, 6, 8, 9, 10)) {
    $data = $stagesData[$stageNum]
    $nextStage = if ($stageNum -lt 10) { $stageNum + 1 } else { "level-select" }
    $nextHref = if ($stageNum -lt 10) { "stage$nextStage.html" } else { "../level-select.html" }
    
    # Generate HTML
    $html = Get-Content $templatePath -Raw
    $html = $html -replace "{STAGE_NUM}", $stageNum
    $html = $html -replace "{STAGE_NAME}", $data.name
    $html = $html -replace "{STAGE_STORY}", $data.story
    $html = $html -replace "{NEXT_HREF}", $nextHref
    $html = $html -replace "{ENEMY_COUNT}", (6 + $stageNum)
    $html = $html -replace "{WORLD_WIDTH}", (2800 + ($stageNum - 2) * 200)
    
    $outputPath = ".\stage$stageNum-refactored.html"
    Set-Content -Path $outputPath -Value $html
    Write-Host "✅ Generated stage$stageNum-refactored.html"
}

Write-Host "All stages generated successfully!"
