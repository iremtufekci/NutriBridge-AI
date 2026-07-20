#!/usr/bin/env python3
"""Türkçe // yorumları anlamlı satırlara ekler; mevcut yorumları ve mantığı değiştirmez."""
import re
import sys
from pathlib import Path

SKIP_PREFIXES = ("//", "///", "#", "/*", "*", "[", "{" , "}", "};", ");", "],", "),")

USING_COMMENTS = {
    "System.": "Sistem kütüphanesi",
    "Microsoft.": "Microsoft framework",
    "MongoDB.": "MongoDB sürücü",
    "Nightbrate.Application.DTOs": "Veri transfer nesneleri",
    "Nightbrate.Application.Exceptions": "Uygulama istisnaları",
    "Nightbrate.Application.Interfaces": "Servis arayüzleri",
    "Nightbrate.Application.Options": "Yapılandırma seçenekleri",
    "Nightbrate.Application.Utils": "Yardımcı araçlar",
    "Nightbrate.Application": "Uygulama katmanı",
    "Nightbrate.Core.Entities": "Varlık sınıfları",
    "Nightbrate.Infrastructure.Data": "Veritabanı bağlamı",
}

def has_trailing_comment(line: str) -> bool:
    # Basit: satırda // varsa (string dışında) yorum var say
    in_str = False
    esc = False
    for i, ch in enumerate(line):
        if esc:
            esc = False
            continue
        if ch == '\\' and in_str:
            esc = True
            continue
        if ch == '"':
            in_str = not in_str
            continue
        if not in_str and line[i:i+2] == '//':
            return True
    return False

def comment_for_using(line: str) -> str:
    inner = line.strip().removeprefix("using ").removesuffix(";").strip()
    for k, v in USING_COMMENTS.items():
        if k in inner:
            return v
    return "Bağımlılık importu"

def comment_for_namespace(line: str) -> str:
    return "Ad alanı tanımı"

def comment_for_class(line: str) -> str:
    m = re.search(r'(?:public|internal|private|protected)?\s*(?:sealed\s+|abstract\s+|static\s+)*class\s+(\w+)', line)
    if m:
        return f"{m.group(1)} sınıfı"
    m = re.search(r'(?:public|internal)\s+(?:sealed\s+)?record\s+(\w+)', line)
    if m:
        return f"{m.group(1)} kayıt tipi"
    return "Sınıf tanımı"

def comment_for_interface(line: str) -> str:
    m = re.search(r'interface\s+(I\w+)', line)
    return f"{m.group(1)} arayüzü" if m else "Arayüz tanımı"

def comment_for_enum(line: str) -> str:
    m = re.search(r'enum\s+(\w+)', line)
    return f"{m.group(1)} enum tanımı" if m else "Enum tanımı"

def comment_for_method(line: str) -> str:
    stripped = line.strip()
    if "=>" in stripped and "(" in stripped:
        return "Metot uygulaması (expression body)"
    if stripped.startswith("public ") or stripped.startswith("private ") or stripped.startswith("protected ") or stripped.startswith("internal "):
        if "(" in stripped and (stripped.endswith("{") or "=>" in stripped):
            m = re.search(r'\)\s*:\s*(\w[\w<>,\s\?]*)', stripped)
            if m:
                return f"Metot: {stripped.split('(')[0].split()[-1]} — dönüş {m.group(1).strip()}"
            m2 = re.search(r'(\w+)\s*\(', stripped)
            if m2:
                return f"{m2.group(1)} metodu"
    return None

def comment_for_property(line: str) -> str:
    m = re.search(r'public\s+([\w<>,\?\[\]]+)\s+(\w+)\s*\{', line)
    if m:
        return f"{m.group(2)} özelliği ({m.group(1)})"
    m = re.search(r'(\w+)\s*\{\s*get;\s*set;\s*\}', line)
    if m:
        return f"{m.group(1)} özelliği"
    return None

def comment_for_field(line: str) -> str:
    m = re.search(r'(?:private|public|protected|internal)\s+(?:readonly\s+|static\s+)*([\w<>,\?\[\]]+)\s+(_?\w+)', line)
    if m:
        return f"{m.group(2)} alanı"
    return None

def comment_for_line(line: str, prev: str) -> str | None:
    stripped = line.strip()
    if not stripped:
        return None
    if has_trailing_comment(line):
        return None
    if stripped.startswith(SKIP_PREFIXES):
        return None
    if stripped in ("{", "}", "};", "};", "],", "),", "});", "});"):
        return None
    if stripped.startswith("using "):
        return comment_for_using(line)
    if stripped.startswith("namespace "):
        return comment_for_namespace(line)
    if " interface " in stripped or stripped.startswith("public interface"):
        return comment_for_interface(line)
    if " enum " in stripped or stripped.startswith("public enum"):
        return comment_for_enum(line)
    if " class " in stripped and ("{" in stripped or stripped.endswith(":")):
        return comment_for_class(line)
    if stripped.startswith("///"):
        return None
    p = comment_for_property(line)
    if p:
        return p
    f = comment_for_field(line)
    if f:
        return f
    m = comment_for_method(line)
    if m:
        return m
    if stripped.startswith("return "):
        return "Sonucu döndür"
    if stripped.startswith("throw "):
        return "Hata fırlat"
    if stripped.startswith("await "):
        return "Asenkron işlem bekle"
    if stripped.startswith("if "):
        return "Koşul kontrolü"
    if stripped.startswith("else if"):
        return "Alternatif koşul"
    if stripped.startswith("else"):
        return "Diğer durum"
    if stripped.startswith("foreach "):
        return "Koleksiyon üzerinde döngü"
    if stripped.startswith("for "):
        return "Döngü"
    if stripped.startswith("switch "):
        return "Çoklu dal seçimi"
    if stripped.startswith("case "):
        return "Dal durumu"
    if stripped.startswith("default:"):
        return "Varsayılan dal"
    if stripped.startswith("var ") and "=" in stripped:
        return "Yerel değişken ataması"
    if stripped.endswith(");") and "=" in stripped:
        return "Atama / çağrı"
    if stripped.endswith(");"):
        return "Metot çağrısı"
    if "=" in stripped and stripped.endswith(";"):
        return "Değer ataması"
    return None

def process_file(path: Path) -> bool:
    text = path.read_text(encoding="utf-8")
    lines = text.splitlines(keepends=True)
    out = []
    changed = False
    prev = ""
    for line in lines:
        raw = line.rstrip("\n\r")
        c = comment_for_line(raw, prev)
        if c and not has_trailing_comment(raw):
            indent = raw[: len(raw) - len(raw.lstrip())]
            body = raw.strip()
            if body.startswith("///"):
                out.append(line)
            else:
                new_line = f"{indent}{body} // {c}\n"
                if new_line.rstrip("\n") != raw:
                    changed = True
                out.append(new_line)
        else:
            out.append(line)
        prev = raw.strip()
    if changed:
        path.write_text("".join(out), encoding="utf-8")
    return changed

def main():
    roots = [
        Path("Nightbrate.Application/Services"),
        Path("Nightbrate.Application/Interfaces"),
        Path("Nightbrate.API/Services"),
    ]
    modified = []
    for root in roots:
        if not root.exists():
            continue
        for p in sorted(root.rglob("*.cs")):
            if "obj" in p.parts or "bin" in p.parts:
                continue
            if process_file(p):
                modified.append(str(p))
    print("\n".join(modified))

if __name__ == "__main__":
    import os
    os.chdir(Path(__file__).resolve().parent)
    main()
