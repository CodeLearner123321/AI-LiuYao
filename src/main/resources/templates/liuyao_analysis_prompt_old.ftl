请分析以下六爻卦：
<#if question?? && question?has_content>
问题：${question}
</#if>
<#if background?? && background?has_content>
背景：${background}
</#if>
<#if timeString?? && timeString?has_content>
时间：${timeString}
</#if>
${guaString}
<#if shenShaString?? && shenShaString?has_content>
${shenShaString}
</#if>
<#list yaoStrings as yaoString>
${yaoString}
</#list>
请你按照如下的分析思路去分析：
1.取用神（可能有一个或多个）
2.根据六亲旺衰、动爻与用神的关系、动变关系、以理法的角度分析事情的吉凶
3.根据六神、神煞、爻位、神煞结合已经分析的理法用象法的角度分析事情的具体过程
4.根据理法和象法两个角度，给出事情的定论。
请你依次按照：1、用神 2、理法  3、象法  4、吉凶定论 5、判辞 这五个标题的格式回复我（判辞就是总结吉凶判断的一句小诗，这句小诗要求通俗易懂，字数不超过12个字）
请你严格参照上述要求分析，要求分析时以专业的角度分析，给出答案要通俗易懂
如果我提交的问题和背景，有误，请直接返回${errorCode}

