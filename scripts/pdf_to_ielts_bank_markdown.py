#!/usr/bin/env python3
"""
从「答案版」PDF 抽取文本，生成 part1.md / part2_part3.md（供管理员粘贴）。
用法：python scripts/pdf_to_ielts_bank_markdown.py
依赖：pip install pypdf
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

try:
    from pypdf import PdfReader
except ImportError:
    print("请安装: pip install pypdf", file=sys.stderr)
    sys.exit(1)

PAGE_MARKER = re.compile(r"^--\s*\d+\s+of\s+\d+\s*--$")
QUESTION_RE = re.compile(
    r"^(Do|Does|Did|Is|Are|Was|Were|Have|Has|Had|Would|Could|Should|Can|Must|May|"
    r"What|Which|Where|When|Who|Whom|Whose|Why|How|If)\b",
    re.I,
)
DESCRIBE_START = re.compile(r"(?m)^Describe\s+(?:a|an|the)\s+.+$")
PART3_HDR = re.compile(r"(?im)^Part\s*3\s*$")


def extract_pdf_text(path: Path) -> str:
    r = PdfReader(str(path))
    return "\n".join((p.extract_text() or "") for p in r.pages)


def norm_spaces(s: str) -> str:
    return " ".join(s.split())


def strip_page_noise(s: str) -> str:
    """去掉 PDF 抽词时误入正文的孤立页码。"""
    t = s.strip()
    t = re.sub(r" (\d{1,2})$", "", t)
    t = re.sub(r" (\d{1,2}) ", " ", t)
    return t


def to_lines(text: str) -> list[str]:
    out: list[str] = []
    for raw in text.replace("\r", "\n").split("\n"):
        t = norm_spaces(raw)
        if not t:
            out.append("")
            continue
        if PAGE_MARKER.match(t.strip()):
            continue
        if re.fullmatch(r"\d{1,3}", t.strip()) and len(t.strip()) <= 3:
            continue
        out.append(t)
    return out


def is_keyword_line(s: str) -> bool:
    t = s.strip()
    if not t:
        return False
    if t.startswith("•") or t.startswith("- ") or t.startswith("* "):
        return True
    if "\uf06c" in t or "" in t:
        return True
    return bool(re.match(r"^[•\-\*]", t))


def clean_kw(s: str) -> str:
    t = s.strip()
    t = re.sub(r"^[\uf06c•\-\*\s]+", "", t)
    return norm_spaces(t)


def is_question_line(s: str) -> bool:
    t = s.strip()
    if len(t) < 3:
        return False
    if t.endswith("?"):
        return True
    return bool(QUESTION_RE.match(t))


def looks_like_topic(line: str, next_nonempty: str) -> bool:
    if not line.strip() or is_question_line(line) or is_keyword_line(line):
        return False
    if len(line) > 180:
        return False
    if not is_question_line(next_nonempty):
        return False
    return bool(re.search(r"[\u4e00-\u9fff]", line)) or "/" in line or line.startswith("Borrowing")


def parse_part1(lines: list[str]) -> str:
    chunks: list[str] = []
    current_topic = "Part 1"
    i = 0
    while i < len(lines):
        nxt = ""
        for k in range(i + 1, len(lines)):
            if lines[k].strip():
                nxt = lines[k]
                break
        if looks_like_topic(lines[i], nxt):
            current_topic = lines[i].strip()
            i += 1
            continue
        if is_question_line(lines[i]):
            q = lines[i].strip()
            i += 1
            ans: list[str] = []
            while i < len(lines):
                if is_keyword_line(lines[i]):
                    break
                if is_question_line(lines[i]):
                    break
                ne = ""
                for k in range(i + 1, len(lines)):
                    if lines[k].strip():
                        ne = lines[k]
                        break
                if looks_like_topic(lines[i], ne):
                    break
                if lines[i].strip().startswith("Describe "):
                    break
                if lines[i].strip():
                    ans.append(lines[i].strip())
                i += 1
            kws: list[str] = []
            while i < len(lines) and is_keyword_line(lines[i]):
                kws.append(clean_kw(lines[i]))
                i += 1
            answer = strip_page_noise(" ".join(ans))
            chunks.append(
                f"## Topic: {current_topic}\n\n"
                f"### Question\n{q}\n\n"
                f"### Answer\n{answer}\n\n"
                f"### Keywords\n"
                + "".join(f"- {k}\n" for k in kws)
                + "\n"
            )
            continue
        i += 1
    return "".join(chunks)


def line_before_describe(full: str, pos: int) -> str:
    pre = full[:pos].rstrip().split("\n")
    skip = {"人物题", "地点类", "物品类", "事件类", "Part 2", "新题"}
    while pre:
        line = norm_spaces(pre[-1]).strip()
        pre.pop()
        if not line:
            continue
        if re.fullmatch(r"\d{1,3}", line):
            continue
        if line in skip or re.match(r"^新题\s*\d+", line):
            continue
        return line
    return "Part 2 Topic"


def split_cue_and_rest(part2_text: str) -> tuple[str, str]:
    """part2_text 从 Describe 开始，到 Part 3 之前。返回 cue_card（含 Describe + You should say 要点）与之后正文。"""
    lines = part2_text.split("\n")
    cue: list[str] = []
    i = 0
    while i < len(lines):
        cue.append(lines[i])
        if re.search(r"(?i)you should say", lines[i]):
            i += 1
            while i < len(lines):
                t = lines[i].strip()
                if not t:
                    i += 1
                    continue
                low = t.lower()
                if (
                    low.startswith("what ")
                    or low.startswith("how ")
                    or low.startswith("when ")
                    or low.startswith("where ")
                    or low.startswith("who ")
                    or low.startswith("why ")
                    or low.startswith("and explain")
                    or low.startswith("and ")
                ):
                    cue.append(lines[i])
                    i += 1
                    continue
                break
            break
        i += 1
    cue_s = "\n".join(norm_spaces(x) for x in cue if x.strip()).strip()
    rest = "\n".join(lines[i:]).strip()
    return cue_s, rest


def split_answer_and_keywords(text: str) -> tuple[str, list[str], str]:
    """正文直到第一个关键词行之前为 answer；连续关键词行；剩余 tail。"""
    lines = text.split("\n")
    i = 0
    ans_lines: list[str] = []
    while i < len(lines):
        if is_keyword_line(lines[i]):
            break
        if lines[i].strip():
            ans_lines.append(lines[i].strip())
        i += 1
    kws: list[str] = []
    while i < len(lines) and is_keyword_line(lines[i]):
        kws.append(clean_kw(lines[i]))
        i += 1
    tail = "\n".join(lines[i:]).strip()
    return strip_page_noise(" ".join(ans_lines)), kws, tail


def parse_part3_section(p3: str) -> list[tuple[str, str, list[str]]]:
    """Part 3 正文：多组 Question / Answer / Keywords"""
    lines = [norm_spaces(l) if l.strip() else "" for l in p3.split("\n")]
    out: list[tuple[str, str, list[str]]] = []
    i = 0
    while i < len(lines):
        while i < len(lines) and not lines[i].strip():
            i += 1
        if i >= len(lines):
            break
        if is_keyword_line(lines[i]):
            i += 1
            continue
        # 题：以 ? 结尾的一行，或常见疑问词开头
        q_parts = [lines[i]]
        i += 1
        while i < len(lines) and lines[i].strip() and not lines[i].strip().endswith("?"):
            if is_keyword_line(lines[i]):
                break
            # 续行
            if not QUESTION_RE.match(lines[i]) and q_parts and not q_parts[-1].endswith("?"):
                q_parts.append(lines[i])
                i += 1
                continue
            break
        if i < len(lines) and lines[i].strip().endswith("?"):
            q_parts.append(lines[i])
            i += 1
        q = " ".join(x.strip() for x in q_parts if x.strip()).strip()
        if not q or DESCRIBE_START.match(q):
            break
        ans_lines: list[str] = []
        while i < len(lines):
            if is_keyword_line(lines[i]):
                break
            t = lines[i].strip()
            if not t:
                i += 1
                continue
            # 下一题（以疑问词开头且带 ?）
            if t.endswith("?") and len(ans_lines) > 3 and q not in t:
                break
            if re.match(r"^[\u4e00-\u9fff]{2,25}$", t) and len(ans_lines) > 8:
                break
            ans_lines.append(t)
            i += 1
        kws: list[str] = []
        while i < len(lines) and is_keyword_line(lines[i]):
            kws.append(clean_kw(lines[i]))
            i += 1
        answer = strip_page_noise(" ".join(ans_lines))
        if q and answer:
            out.append((q, answer, kws))
    return out


def format_one_topic_block(full: str, start: int, end: int) -> str:
    block = full[start:end].strip()
    topic = line_before_describe(full, start)
    p3m = PART3_HDR.search(block)
    if p3m:
        p2_text = block[: p3m.start()].strip()
        p3_text = block[p3m.end() :].strip()
    else:
        p2_text = block
        p3_text = ""

    cue, rest_after_cue = split_cue_and_rest(p2_text)
    answer, p2_kw, tail = split_answer_and_keywords(rest_after_cue)
    # tail 应为空；若关键词被算进 tail 再合并
    if not p2_kw and tail:
        _, p2_kw, tail = split_answer_and_keywords(tail)

    p3_qas = parse_part3_section(p3_text) if p3_text else []

    lines_out: list[str] = [
        f"## Topic: {topic}\n",
        "\n### Part 2\n\n",
        "#### Cue card\n",
        cue + "\n\n",
        "#### Answer\n",
        strip_page_noise(answer) + "\n\n",
        "#### Keywords\n",
    ]
    for k in p2_kw:
        lines_out.append(f"- {k}\n")
    lines_out.append("\n### Part 3\n\n")
    for q, a, kw in p3_qas:
        lines_out.append("#### Question\n" + strip_page_noise(q) + "\n\n")
        lines_out.append("#### Answer\n" + strip_page_noise(a) + "\n\n")
        lines_out.append("#### Keywords\n")
        for k in kw:
            lines_out.append(f"- {k}\n")
        lines_out.append("\n")
    return "".join(lines_out)


def describe_starts(text: str) -> list[int]:
    """去重：相同 Describe 开头只保留第一次出现"""
    seen: set[str] = set()
    starts: list[int] = []
    for m in DESCRIBE_START.finditer(text):
        key = text[m.start() : m.start() + 100]
        if key in seen:
            continue
        seen.add(key)
        starts.append(m.start())
    return starts


def build_part23(full: str, part1_end: int) -> str:
    body = full[part1_end:]
    starts = describe_starts(body)
    parts: list[str] = []
    for i, s in enumerate(starts):
        e = starts[i + 1] if i + 1 < len(starts) else len(body)
        parts.append(format_one_topic_block(full, part1_end + s, part1_end + e))
    return "\n".join(parts)


def main() -> None:
    root = Path(__file__).resolve().parents[1]
    pdf = root / "IELTS speaking test bank" / "【答案版】2025年9-12月口语新题完整版.pdf"
    if not pdf.exists():
        print(f"找不到 PDF: {pdf}", file=sys.stderr)
        sys.exit(1)
    raw = extract_pdf_text(pdf)
    m = DESCRIBE_START.search(raw)
    if not m:
        print("未找到 Describe 题干", file=sys.stderr)
        sys.exit(1)
    part1_end = m.start()
    lines = to_lines(raw[:part1_end])
    start_idx = 0
    for idx, ln in enumerate(lines):
        if "Quiet" in ln and "Noisy" in ln:
            start_idx = idx
            break
    part1_md = parse_part1(lines[start_idx:])
    part23_md = build_part23(raw, part1_end)

    out1 = root / "IELTS speaking test bank" / "2025-9-12-答案版-part1.md"
    out2 = root / "IELTS speaking test bank" / "2025-9-12-答案版-part2-part3.md"
    out1.write_text(part1_md, encoding="utf-8")
    out2.write_text(part23_md, encoding="utf-8")
    print(f"Wrote {out1} ({len(part1_md)} chars)")
    print(f"Wrote {out2} ({len(part23_md)} chars)")


if __name__ == "__main__":
    main()
