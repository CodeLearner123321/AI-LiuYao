<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8" />
    <title>${title}</title>
    <style>
        * {
            box-sizing: border-box;
        }

        html,
        body {
            margin: 0;
            padding: 0;
            width: ${posterWidth}px;
            height: ${posterHeight}px;
            overflow: hidden;
            font-family: "STSong", "SimSun", "Songti SC", serif;
            color: #5b3a29;
            background: #f7f1e5;
        }

        body {
            background:
                radial-gradient(circle at 20% 18%, rgba(140, 106, 74, 0.08), transparent 22%),
                radial-gradient(circle at 78% 16%, rgba(91, 58, 41, 0.05), transparent 18%),
                radial-gradient(circle at 82% 82%, rgba(140, 106, 74, 0.08), transparent 22%),
                linear-gradient(180deg, rgba(255,255,255,0.72), rgba(247,241,229,0.92)),
                url("${backgroundImageUrl}") center/cover no-repeat;
        }

        .poster {
            position: relative;
            width: ${posterWidth}px;
            height: ${posterHeight}px;
            padding: 66px 88px 36px;
        }

        .title-wrap {
            text-align: center;
            margin-bottom: 26px;
        }

        .title-sub {
            font-size: 22px;
            letter-spacing: 9px;
            color: #8c6a4a;
            margin-bottom: 12px;
        }

        .title-main {
            margin: 0;
            font-family: "STKaiti", "KaiTi", serif;
            font-size: 64px;
            letter-spacing: 9px;
            color: #5b3a29;
            line-height: 1.15;
        }

        .title-line {
            width: 280px;
            height: 1px;
            margin: 18px auto 0;
            background: linear-gradient(90deg, transparent, rgba(140,106,74,0.68), transparent);
        }

        .section {
            margin-bottom: 18px;
        }

        .label {
            font-family: "STKaiti", "KaiTi", serif;
            font-size: 30px;
            color: #7a5339;
            margin-bottom: 6px;
            letter-spacing: 2px;
        }

        .inline-row {
            display: flex;
            align-items: baseline;
            gap: 10px;
            flex-wrap: nowrap;
        }

        .inline-label {
            flex: 0 0 auto;
            font-family: "STKaiti", "KaiTi", serif;
            font-size: 30px;
            color: #7a5339;
            letter-spacing: 2px;
            white-space: nowrap;
        }

        .inline-value {
            flex: 1 1 auto;
            font-size: 29px;
            line-height: 1.8;
            color: #4d3224;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
        }

        .cast-time {
            font-size: 31px;
            line-height: 1.8;
            color: #4d3224;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
        }

        .ganzhi-time {
            font-family: "STKaiti", "KaiTi", serif;
            font-size: 42px;
            line-height: 1.55;
            color: #5b3a29;
            margin-top: 8px;
            text-align: center;
            display: flex;
            justify-content: center;
            gap: 58px;
            white-space: nowrap;
        }

        .ganzhi-token {
            display: inline-block;
        }

        .shensha-line {
            font-size: 24px;
            line-height: 1.8;
            color: #6d4a34;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
        }

        .bagua-grid {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 54px;
            margin-top: 16px;
        }

        .bagua-grid.single {
            display: flex;
            justify-content: center;
            gap: 0;
        }

        .bagua-grid.single .bagua-block {
            width: min(900px, 100%);
            margin: 0;
        }

        .bagua-grid.single .yao-stack {
            align-items: center;
        }

        .bagua-grid.single .yao-item {
            width: 760px;
        }

        .bagua-grid.single .yao-row {
            grid-template-columns: 86px 260px 300px;
            justify-content: center;
        }

        .bagua-grid.single .yao-fu {
            width: 300px;
        }

        .bagua-block-title {
            text-align: center;
            font-family: "STKaiti", "KaiTi", serif;
            font-size: 34px;
            color: #5c3a27;
            margin: 4px 0 18px;
            letter-spacing: 3px;
        }

        .yao-stack {
            display: flex;
            flex-direction: column;
            gap: 4px;
        }

        .yao-item {
            height: 92px;
            display: flex;
            flex-direction: column;
            justify-content: flex-start;
        }

        .yao-row {
            display: grid;
            grid-template-columns: 86px 260px 1fr;
            align-items: center;
            height: 64px;
            column-gap: 14px;
            padding: 4px 0;
        }

        .yao-left {
            text-align: center;
        }

        .yao-liu-shen {
            font-size: 23px;
            color: #62402d;
        }

        .yao-mid {
            display: flex;
            align-items: center;
            justify-content: center;
        }

        .yao-line-wrap {
            display: flex;
            align-items: center;
            gap: 10px;
        }

        .yao-line.yang {
            width: 168px;
            height: 12px;
            border-radius: 999px;
            background: linear-gradient(90deg, #5b3a29 0%, #8c6a4a 100%);
        }

        .yao-line.yin {
            width: 168px;
            display: flex;
            justify-content: space-between;
        }

        .yao-line.yin span {
            width: 70px;
            height: 12px;
            border-radius: 999px;
            background: linear-gradient(90deg, #5b3a29 0%, #8c6a4a 100%);
        }

        .relation-marks {
            min-width: 46px;
            display: flex;
            align-items: center;
            gap: 6px;
        }

        .relation-mark {
            font-family: "STKaiti", "KaiTi", serif;
            font-size: 24px;
            line-height: 1;
            color: #5a3a29;
        }

        .relation-mark.moving {
            font-size: 25px;
            color: #8b4d2b;
        }

        .relation-mark.shiying {
            font-size: 23px;
            color: #5a3a29;
        }

        .relation-mark.empty {
            visibility: hidden;
        }

        .yao-right {
            font-size: 24px;
            line-height: 1.65;
            color: #4e3225;
            word-break: break-word;
        }

        .yao-fu {
            margin-left: 364px;
            margin-top: 0;
            height: 22px;
            font-size: 19px;
            line-height: 22px;
            color: #755039;
            font-style: italic;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
        }

        .result-section {
            margin-top: 26px;
        }

        .result-title {
            margin: 0 0 12px;
            text-align: center;
            font-family: "STKaiti", "KaiTi", serif;
            font-size: 42px;
            letter-spacing: 6px;
            color: #6f4a35;
            line-height: 1.3;
        }

        .result-outcome {
            margin-bottom: 16px;
            text-align: center;
            font-family: "STKaiti", "KaiTi", serif;
            font-size: 31px;
            line-height: 1.8;
            color: #7f5a42;
            letter-spacing: 2px;
        }

        .analysis-body {
            font-size: 27px;
            line-height: 1.9;
            color: #4d3224;
            white-space: pre-wrap;
            word-break: break-word;
        }
    </style>
</head>
<body>
<div class="poster">
    <header class="title-wrap">
        <div class="title-sub"></div>
        <h1 class="title-main">${title}</h1>
        <div class="title-line"></div>
    </header>

    <section class="section">
        <div class="inline-row">
            <div class="inline-label">起卦时间：</div>
            <div class="cast-time">${castTimeText}</div>
        </div>
    </section>

    <section class="section">
        <div class="inline-row">
            <div class="inline-label">问题：</div>
            <div class="inline-value">${question}</div>
        </div>
    </section>

    <#if background?? && background?has_content>
    <section class="section">
        <div class="inline-row">
            <div class="inline-label">问题背景：</div>
            <div class="inline-value">${background}</div>
        </div>
    </section>
    </#if>

    <section class="section">
        <div class="ganzhi-time">
            <#list ganzhiTokens as token>
                <span class="ganzhi-token">${token}</span>
            </#list>
        </div>
    </section>

    <#if shenShaList?? && shenShaList?size gt 0>
    <section class="section">
        <div class="inline-row">
            <div class="inline-label">神煞：</div>
            <div class="shensha-line">
            <#list shenShaList as shenSha>
                ${shenSha}<#if shenSha_has_next>　</#if>
            </#list>
            </div>
        </div>
    </section>
    </#if>

    <section class="section">
        <div class="bagua-grid <#if !hasChanged>single</#if>">
            <div class="bagua-block">
                <div class="bagua-block-title">主卦 · ${originalName}</div>
                <div class="yao-stack">
                    <#list originalYaoRows as row>
                        <div class="yao-item <#if row.moving>moving</#if>">
                            <div class="yao-row">
                                <div class="yao-left">
                                    <div class="yao-liu-shen">${row.liuShen}</div>
                                </div>
                                <div class="yao-mid">
                                    <div class="yao-line-wrap">
                                        <#if row.yang>
                                            <div class="yao-line yang"></div>
                                        <#else>
                                            <div class="yao-line yin"><span></span><span></span></div>
                                        </#if>
                                        <div class="relation-marks">
                                            <#if row.shiMarker?? && row.shiMarker?has_content>
                                                <span class="relation-mark shiying">${row.shiMarker}</span>
                                            </#if>
                                            <#if row.movingMarker?? && row.movingMarker?has_content>
                                                <span class="relation-mark moving">${row.movingMarker}</span>
                                            </#if>
                                            <#if (!row.shiMarker?? || !row.shiMarker?has_content) && (!row.movingMarker?? || !row.movingMarker?has_content)>
                                                <span class="relation-mark empty">.</span>
                                            </#if>
                                        </div>
                                    </div>
                                </div>
                                <div class="yao-right">${row.liuQin} ${row.tianGanDiZhi}</div>
                            </div>
                            <div class="yao-fu">
                                <#if row.fuShen?? && row.fuShen?has_content>伏神：${row.fuShen}</#if>
                            </div>
                        </div>
                    </#list>
                </div>
            </div>

            <#if hasChanged>
            <div class="bagua-block">
                <div class="bagua-block-title">变卦 · ${changedName}</div>
                <div class="yao-stack">
                    <#list changedYaoRows as row>
                        <div class="yao-item">
                            <div class="yao-row">
                                <div class="yao-left">
                                    <div class="yao-liu-shen">${row.liuShen}</div>
                                </div>
                                <div class="yao-mid">
                                    <div class="yao-line-wrap">
                                        <#if row.yang>
                                            <div class="yao-line yang"></div>
                                        <#else>
                                            <div class="yao-line yin"><span></span><span></span></div>
                                        </#if>
                                    </div>
                                </div>
                                <div class="yao-right">${row.liuQin} ${row.tianGanDiZhi}</div>
                            </div>
                            <div class="yao-fu">
                                <#if row.fuShen?? && row.fuShen?has_content>伏神：${row.fuShen}</#if>
                            </div>
                        </div>
                    </#list>
                </div>
            </div>
            </#if>
        </div>
    </section>

    <section class="section result-section">
        <h2 class="result-title">测算结果</h2>
        <#if keyOutcome?? && keyOutcome?has_content>
        <div class="result-outcome">${keyOutcome}</div>
        </#if>
        <div class="analysis-body">${analysisText}</div>
    </section>
</div>
</body>
</html>
