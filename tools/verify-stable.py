"""Check stable documentation against the tagged Volan public ABI and example sources."""
import argparse
import json
from pathlib import Path
import re
import subprocess

parser = argparse.ArgumentParser()
parser.add_argument('--source', type=Path, required=True, help='Volan source Git checkout')
args = parser.parse_args()
root = Path(__file__).resolve().parents[1]
coverage = json.loads((root/'tools/stable-coverage.json').read_text(encoding='utf-8'))
config = json.loads((root/'docs.json').read_text(encoding='utf-8'))
version = 'v'+coverage['version']
errors = []

def check(condition, message):
    if not condition: errors.append(message)

def source(path):
    return subprocess.check_output(['git', 'show', coverage['sourceCommit']+':'+path], cwd=args.source).decode('utf-8')

def blocks(text):
    return re.findall(r'```([^\n]*)\n(.*?)\n```',text,re.S)

def walk(groups):
    for item in groups:
        if isinstance(item,str): yield item
        else:
            yield from walk(item.get('pages',[]))
            yield from walk(item.get('groups',[]))

navigation=set()
for language in config['navigation']['languages']:
    for release in language['versions']:
        for path in walk(release['groups']):
            navigation.add(path)
            check((root/(path+'.mdx')).exists(),'Missing navigation page: '+path)

pages={}
for topic in coverage['topics']:
    for lang in ('ru','en'):
        path=f'{version}/{lang}/{topic}'
        file=root/(path+'.mdx')
        check(file.exists(),'Missing topic: '+path)
        check(path in navigation,'Topic missing from navigation: '+path)
        if file.exists(): pages[path]=file.read_text(encoding='utf-8')
    ru=pages.get(f'{version}/ru/{topic}','')
    en=pages.get(f'{version}/en/{topic}','')
    check(blocks(ru)==blocks(en),'Different RU/EN code blocks: '+topic)

total_types=0; total_members=0
for module in coverage['modules']:
    dump=source(module['api'])
    contracts=re.findall(r'(?m)^(public [^\n]*class (\S+)[^\n]*\n.*?^\})',dump,re.S)
    check(len(contracts)==module['types'],'Type count mismatch: '+module['module'])
    check(set(symbol for _,symbol in contracts)==set(module['symbols']),'Type mapping mismatch: '+module['module'])
    for contract,symbol in contracts:
        path=module['symbols'].get(symbol)
        if not path: continue
        for lang in ('ru','en'):
            check(contract in pages.get(f'{version}/{lang}/{path}',''),'Missing public contract: '+symbol+' '+lang)
        total_types+=1
        total_members+=len(re.findall(r'(?m)^\s+(?:public|protected) ',contract))

redirects={r['source']:r['destination'] for r in config['redirects']}
for origin,destination in redirects.items():
    if destination.startswith('/'):
        clean=destination.split('#')[0].split('?')[0].strip('/')
        check('/'+clean in redirects or (root/(clean+'.mdx')).exists(),'Missing redirect target: '+origin+' -> '+destination)

project=root/'examples/first-query-1.0.0'
for lang in ('ru','en'):
    quickstart=pages[f'{version}/{lang}/quickstart']
    files=[('settings.gradle.kts','kotlin'),('build.gradle.kts','kotlin'),('schema.volan','prisma'),('migrations/20261005000000_initial/migration.sql','sql'),('src/main/kotlin/org/example/Main.kt','kotlin'),('src/main/java/Main.java','java')]
    snippets=[(header.split(' ')[0],body.strip()) for header,body in blocks(quickstart)]
    for name,syntax in files:
        text=(project/name).read_text(encoding='utf-8').strip()
        check((syntax,text) in snippets,'Quickstart differs from '+name+' '+lang)

if errors:
    print('\n'.join(errors))
    raise SystemExit(1)
print(f'OK: {len(coverage["topics"])} topics per language, {len(coverage["modules"])} modules, {total_types} public JVM types, {total_members} public/protected members; navigation, redirects and quickstarts match.')
