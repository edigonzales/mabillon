#!/usr/bin/env python3
from pathlib import Path
import re, sys
root=Path(__file__).resolve().parents[1]/'.agents'/'skills'
name_re=re.compile(r'^[a-z0-9]+(?:-[a-z0-9]+)*$')
errors=[]
for d in sorted(p for p in root.iterdir() if p.is_dir()):
    f=d/'SKILL.md'
    if not f.exists(): errors.append(f'{d}: missing SKILL.md'); continue
    txt=f.read_text()
    if not txt.startswith('---\n'): errors.append(f'{f}: missing YAML frontmatter'); continue
    parts=txt.split('---\n',2)
    if len(parts)<3: errors.append(f'{f}: unterminated YAML frontmatter'); continue
    fm=parts[1]
    m=re.search(r'^name:\s*(.+)\s*$',fm,re.M)
    desc=re.search(r'^description:\s*(.+)\s*$',fm,re.M)
    if not m: errors.append(f'{f}: missing name'); continue
    name=m.group(1).strip().strip('"\'')
    if name != d.name: errors.append(f'{f}: name {name!r} != dir {d.name!r}')
    if not name_re.fullmatch(name): errors.append(f'{f}: invalid name')
    if not desc or not desc.group(1).strip(): errors.append(f'{f}: missing description')
if errors:
    print('\n'.join(errors), file=sys.stderr); sys.exit(1)
print(f'OK: {len(list(root.glob("*/SKILL.md")))} skills')
