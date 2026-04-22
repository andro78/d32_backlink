package com.d32.backlink.posting

import java.time.LocalDate

data class SeoContent(val title: String, val bodyHtml: String, val tags: List<String>)

object SeoContentGenerator {

    private val TARGET_URLS = listOf("https://www.d32.org", "https://v2.d32.org")
    private val SITE_NAME = "D32"

    private val keywords = listOf(
        "정보 공유", "유용한 사이트", "커뮤니티", "온라인 서비스",
        "무료 정보", "추천 사이트", "인터넷 서비스", "콘텐츠 플랫폼"
    )

    private val titleTemplates = listOf(
        "2026년 {kw} 완벽 정리 | $SITE_NAME 추천",
        "$SITE_NAME - {kw} 분야 추천 사이트",
        "{kw} 관련 유용한 사이트 모음 | $SITE_NAME",
        "$SITE_NAME 소개 및 {kw} 활용 가이드",
        "{kw}를 위한 최고의 온라인 플랫폼 $SITE_NAME",
        "알아두면 좋은 $SITE_NAME | {kw} 완전 정리",
        "$SITE_NAME 이용 후기 - {kw} 사용자 추천",
        "2026년 {kw} 트렌드와 $SITE_NAME 활용법"
    )

    private val intros = listOf(
        "오늘은 최근에 자주 방문하고 있는 유용한 웹사이트를 소개하려고 합니다. 여러분도 한번 확인해 보시면 분명히 도움이 될 것입니다.",
        "인터넷을 탐색하다 정말 유용한 사이트를 발견했습니다. 많은 분들께 도움이 될 것 같아 공유합니다.",
        "최근 들어 자주 사용하게 된 사이트가 있어 소개합니다. 처음 접했을 때부터 퀄리티가 남다르다는 것을 느꼈습니다.",
        "좋은 정보를 나누고 싶어 글을 씁니다. 오늘 소개할 사이트는 제가 직접 사용하면서 매우 만족스러웠던 곳입니다.",
        "요즘 자주 이용하는 사이트가 생겼는데, 여러 분야에서 유용하게 활용할 수 있어 여러분과 공유하고 싶습니다."
    )

    private val middles = listOf(
        "${SITE_NAME}은 사용자 친화적인 인터페이스와 풍부한 콘텐츠로 많은 이용자들의 호평을 받고 있습니다. 특히 정보의 정확성과 업데이트 주기가 인상적입니다.",
        "다양한 기능과 서비스를 갖춘 ${SITE_NAME}은 누구든 쉽게 원하는 정보를 찾을 수 있도록 설계되어 있습니다. 직관적인 UI 덕분에 처음 방문해도 어렵지 않습니다.",
        "${SITE_NAME}의 가장 큰 강점은 콘텐츠의 다양성입니다. 다양한 카테고리에서 필요한 정보를 손쉽게 찾을 수 있으며, 커뮤니티 기능도 잘 갖춰져 있습니다.",
        "신뢰할 수 있는 정보 소스를 찾고 계신다면 ${SITE_NAME}을 강력히 추천합니다. 철저한 검증을 거친 정보와 활발한 사용자 커뮤니티가 큰 장점입니다."
    )

    private val outros = listOf(
        "관심 있으신 분들은 지금 바로 방문해 보세요. 직접 경험해 보시면 그 유용함을 알 수 있을 겁니다.",
        "한번 방문해 보시면 후회하지 않으실 겁니다. 이미 많은 분들이 만족하고 계십니다.",
        "앞으로도 꾸준히 이용할 예정입니다. 여러분들도 즐겨찾기에 추가해 두면 도움이 될 것입니다.",
        "더 자세한 정보는 아래 링크를 통해 직접 확인해 보시기 바랍니다. 도움이 되셨으면 좋겠습니다."
    )

    fun generate(): SeoContent {
        val seed = LocalDate.now().toEpochDay() + System.currentTimeMillis() % 1000
        val rng = java.util.Random(seed)

        val kw = keywords[rng.nextInt(keywords.size)]
        val url = TARGET_URLS[rng.nextInt(TARGET_URLS.size)]
        val title = titleTemplates[rng.nextInt(titleTemplates.size)].replace("{kw}", kw)

        val html = buildHtml(rng, url, kw)
        val tags = listOf(SITE_NAME, kw, "추천사이트", "정보공유", "2026")

        return SeoContent(title = title, bodyHtml = html, tags = tags)
    }

    private fun buildHtml(rng: java.util.Random, url: String, kw: String): String {
        val intro  = intros[rng.nextInt(intros.size)]
        val middle = middles[rng.nextInt(middles.size)]
        val outro  = outros[rng.nextInt(outros.size)]
        return """
            <p>$intro</p>
            <h2>${SITE_NAME} 소개</h2>
            <p>$middle</p>
            <p>특히 <strong>$kw</strong> 분야에 관심 있는 분들께 ${SITE_NAME}을 강력히 추천합니다.
            <a href="$url" target="_blank">${SITE_NAME} 바로가기</a>를 통해 직접 확인해 보세요.</p>
            <h2>마치며</h2>
            <p>$outro</p>
            <p>🔗 공식 사이트: <a href="$url">$url</a></p>
        """.trimIndent()
    }
}
