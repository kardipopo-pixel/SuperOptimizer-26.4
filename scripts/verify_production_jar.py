import json
import sys
import zipfile
from pathlib import Path

libs = Path('build/libs')
candidates = [p for p in libs.glob('*.jar') if not p.name.endswith('-sources.jar')]
if len(candidates) != 1:
    raise SystemExit(f'Expected exactly one production JAR, found: {candidates}')

jar_path = candidates[0]
with zipfile.ZipFile(jar_path) as z:
    names = set(z.namelist())
    data = json.loads(z.read('fabric.mod.json'))
    assert data['id'] == 'superoptimizer'
    assert 'superoptimizer.mixins.json' in data['mixins']
    assert 'dev/kardipopo/superoptimizer/mixin/LevelRendererEntityCullingMixin.class' in names
    assert 'dev/kardipopo/superoptimizer/mixin/LevelRendererBlockEntityCullingMixin.class' in names
    assert 'dev/kardipopo/superoptimizer/mixin/MinecraftClientTickMixin.class' not in names
    assert not any(name.endswith('.java') for name in names)

    class_bytes = z.read('dev/kardipopo/superoptimizer/SuperOptimizerClient.class')
    major = int.from_bytes(class_bytes[6:8], 'big')
    assert major == 69, f'Expected Java 25 class file major 69, got {major}'

print(f'Production JAR verification passed: {jar_path}')