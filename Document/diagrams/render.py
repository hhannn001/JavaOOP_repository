"""Render class Mermaid subset to DOT; render all DOT sources to SVG and PNG.

Run from any directory: python3 Document/diagrams/render.py
Requires Graphviz dot on PATH. This supports only the classDiagram syntax used here.
"""
from pathlib import Path
import html
import re
import subprocess

ROOT = Path(__file__).resolve().parent


def class_dot(source):
    nodes   = {}
    edges   = []
    current = None
    for line in source.splitlines():
        line = line.strip()
        if not line or line == 'classDiagram':
            continue
        match = re.fullmatch(r'class (\w+)(\s*\{)?', line)
        if match:
            current = match[1]
            nodes[current] = []
            if not match[2]:
                current = None
        elif line == '}':
            current = None
        elif current:
            nodes[current].append(line)
        else:
            match = re.fullmatch(r'(\w+)\s+(\.\.\|>|\.\.>|-->|\*--|o--)\s+(\w+)(?:\s*:\s*(.*))?', line)
            if not match:
                raise ValueError(f'Unsupported Mermaid line: {line}')
            edges.append(match.groups())
    output = ['digraph G {',
              'graph [rankdir=TB, bgcolor="white", pad=0.3, nodesep=0.35, ranksep=0.6, dpi=110];',
              'node [shape=plain, fontname="Helvetica"];',
              'edge [fontname="Helvetica", fontsize=10, color="#627086"];']
    for name, members in nodes.items():
        rows = ''.join('<TR><TD ALIGN="LEFT">' + html.escape(re.sub(r'~([^~]+)~', r'<\1>', member)) + '</TD></TR>'
                       for member in members)
        output.append(f'{name} [label=<<TABLE BORDER="1" CELLBORDER="0" CELLSPACING="0" CELLPADDING="7">'
                      f'<TR><TD BGCOLOR="#dde7f5"><B>{name}</B></TD></TR>{rows}</TABLE>>];')
    styles = {'..|>': 'style=dashed, arrowhead=empty', '..>': 'style=dashed, arrowhead=vee',
              '-->': 'arrowhead=vee', '*--': 'dir=both, arrowtail=diamond, arrowhead=none',
              'o--': 'dir=both, arrowtail=odiamond, arrowhead=none'}
    for start, relation, end, label in edges:
        if start not in nodes or end not in nodes:
            raise ValueError(f'Undeclared class: {start} -> {end}')
        suffix = f', label="{label}"' if label else ''
        output.append(f'{start} -> {end} [{styles[relation]}{suffix}];')
    return '\n'.join(output + ['}', ''])


def main():
    for source in sorted(ROOT.glob('0[123]_*.mmd')):
        source.with_suffix('.dot').write_text(class_dot(source.read_text()))
    for source in sorted(ROOT.glob('*.dot')):
        for extension in ('svg', 'png'):
            subprocess.run(['dot', f'-T{extension}', str(source), '-o', str(source.with_suffix('.' + extension))],
                           check=True)
        print(source.stem)


if __name__ == '__main__':
    main()
