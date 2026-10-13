// Verify the Java mount pipeline, which is separate from the GeckoLib creature exporter.
const fs = require('node:fs'), assert = require('node:assert/strict');
const ids = ['woolly_rhino','flying_squirrel','poptart_cat','dragon_mount','winged_unicorn','riding_golem','sasquatch','cerberus','leopard','seahorse','giant_scorpion','crocodile','giant_mole'];
let cubes = 0;
for (const id of ids) {
  const name = id.split('_').map(s => s[0].toUpperCase() + s.slice(1)).join('');
  const model = JSON.parse(fs.readFileSync(`art/blockbench/${id}/${name}.bbmodel`));
  const java = fs.readFileSync(`src/client/java/darkspawn/black/client/mount/${name}Model.java`, 'utf8');
  const texture = fs.readFileSync(`src/main/resources/assets/darkspawn/textures/entity/${id}.png`);
  assert.equal(model.textures[0].source, 'data:image/png;base64,' + texture.toString('base64'), id + ' embedded texture parity');
  assert.equal(texture.readUInt32BE(16), 128); assert.equal(texture.readUInt32BE(20), 128);
  assert.equal(model.elements.length, [...java.matchAll(/\.addBox\(/g)].length, id + ' Java/source cube count');
  const names = new Set(model.groups.map(g => g.name));
  if (['dragon_mount','woolly_rhino','giant_scorpion','crocodile'].includes(id)) {
    const attack = model.animations.find(a => a.name.endsWith('.attack'));
    assert(attack && attack.loop === 'once', id + ' editable attack clip');
    assert.equal(attack.length, id === 'woolly_rhino' ? 1 : id === 'dragon_mount' ? .6 : .4);
    let moving = false;
    for (const animator of Object.values(attack.animators)) {
      assert(names.has(animator.name), id + ' attack bone exists');
      for (const channel of ['rotation', 'position']) {
        const frames = animator.keyframes.filter(k => k.channel === channel);
        assert(frames.every((k,i) => Number.isFinite(k.time) && k.time >= 0 && k.time <= attack.length
          && (!i || k.time >= frames[i-1].time)), id + ' ordered attack frames');
        for (const frame of frames) {
          const values = Object.values(frame.data_points[0]).map(Number);
          assert(values.every(Number.isFinite), id + ' finite attack transform');
          if (values.some(v => v !== 0)) moving = true;
        }
        assert(Object.values(frames.at(-1).data_points[0]).every(v => Number(v) === 0), id + ' recovery returns to rest');
      }
    }
    assert(moving, id + ' attack must animate');
  }
  for (const [, part] of java.matchAll(/getChild\("([a-z_]+)"\)/g)) assert(names.has(part), id + ' missing animated part ' + part);
  for (const cube of model.elements) {
    const [w,h,d] = cube.to.map((v,i) => v - cube.from[i]), [u,v] = cube.uv_offset;
    assert([...cube.from,...cube.to,u,v].every(Number.isFinite), id + ' non-finite cube');
    assert(w > 0 && h > 0 && d > 0, id + ' empty cube');
    assert(u >= 0 && v >= 0 && u + 2 * (w+d) <= 128 && v + d+h <= 128, id + ' UV overflow');
    cubes++;
  }
}
console.log(`PASS: ${ids.length} mount projects, ${cubes} cubes; Java counts, animated parts, UV bounds and embedded texture parity.`);
