// Replace placeholder glyphs with editable vectors derived from AppIcons
// in app/src/main/java/com/letitout/app/ui/AppVisuals.kt.
const page=figma.root.children.find(p=>p.id==='10:88');
await figma.setCurrentPageAsync(page);
await figma.loadFontAsync({family:'Fredoka',style:'SemiBold'});
await figma.loadFontAsync({family:'Nunito',style:'Bold'});
const created=[], removed=[], mutated=[];
const C={ink:'#26243D',muted:'#5E5B7A',purple:'#6A5AE0',white:'#FFFFFF',angry:'#C9633F',sad:'#3F7FC9',stressed:'#1F8C72',anxious:'#2A8BA3',lonely:'#C9507F',happy:'#A67C00',green:'#238D7C'};
const paths={
  heart:{solid:true,d:['M12 21c-7-4.5-10-8.5-10-13a5 5 0 0 1 9-3 5 5 0 0 1 9 3c0 4.5-3 8.5-8 13z']},
  back:{d:['M15 5l-7 7 7 7']},
  settings:{d:['M12 9a3 3 0 1 0 0 6 3 3 0 0 0 0-6z','M19.4 15a1.7 1.7 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-1.8-.3 1.7 1.7 0 0 0-1 1.5V21a2 2 0 1 1-4 0v-.1a1.7 1.7 0 0 0-1.1-1.5 1.7 1.7 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.7 1.7 0 0 0 .3-1.8 1.7 1.7 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.1a1.7 1.7 0 0 0 1.5-1.1 1.7 1.7 0 0 0-.3-1.8l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.7 1.7 0 0 0 1.8.3H9a1.7 1.7 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.1a1.7 1.7 0 0 0 1 1.5 1.7 1.7 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.7 1.7 0 0 0-.3 1.8V9a1.7 1.7 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.1a1.7 1.7 0 0 0-1.5 1z']},
  flame:{d:['M12 3s5 4 5 9.5a5 5 0 0 1-10 0c0-2 1-3.5 2-4.5.5 1.5 1.5 2.5 2 2.5.5-2 0-4.5 1-7.5z']},
  drop:{d:['M12 3s6 6.5 6 11a6 6 0 0 1-12 0c0-4.5 6-11 6-11z']},
  waves:{d:['M3 9l3-3 3 3 3-3 3 3 3-3 3 3','M3 16l3-3 3 3 3-3 3 3 3-3 3 3']},
  spiral:{d:['M12 12a2 2 0 0 1 2 2 3 3 0 0 1-5 2.2 5 5 0 0 1 3-8.2 7 7 0 0 1 7 7']},
  moon:{d:['M20 14.5A8 8 0 1 1 9.5 4a6.5 6.5 0 0 0 10.5 10.5z']},
  sun:{d:['M12 8a4 4 0 1 0 0 8 4 4 0 0 0 0-8z','M12 2v2M12 20v2M2 12h2M20 12h2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4']},
  chat:{d:['M4 6a2 2 0 0 1 2-2h12a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H9l-5 4V6z']},
  mic:{d:['M9 6a3 3 0 0 1 6 0v6a3 3 0 0 1-6 0V6z','M5 11a7 7 0 0 0 14 0M12 18v3']},
  send:{d:['M12 19V5M6 11l6-6 6 6']},
  shield:{d:['M12 3l8 3v6c0 5-3.5 8.5-8 9-4.5-.5-8-4-8-9V6l8-3z']},
  phone:{d:['M5 4h4l2 5-2.5 1.5a11 11 0 0 0 5 5L15 13l5 2v4a2 2 0 0 1-2 2A16 16 0 0 1 3 6a2 2 0 0 1 2-2z']},
  sync:{d:['M4 12a8 8 0 0 1 14-5.3L20 9M20 4v5h-5','M20 12a8 8 0 0 1-14 5.3L4 15M4 20v-5h5']},
  storage:{d:['M7 5h10a3 3 0 0 1 3 3v8a3 3 0 0 1-3 3H7a3 3 0 0 1-3-3V8a3 3 0 0 1 3-3z','M4 11h16']},
  trash:{d:['M4 7h16M10 11v6M14 11v6M6 7l1 13h10l1-13M9 7V4h6v3']},
  people:{d:['M9 4.5a3.5 3.5 0 1 0 0 7 3.5 3.5 0 0 0 0-7z','M2.5 20a6.5 6.5 0 0 1 13 0M16 4a3.5 3.5 0 0 1 0 7M21.5 20a6.5 6.5 0 0 0-4.5-6.2']},
  more:{solid:true,shapes:'<circle cx="5" cy="12" r="2"/><circle cx="12" cy="12" r="2"/><circle cx="19" cy="12" r="2"/>'}
};
function svg(type,color,size){
  const p=paths[type];
  const body=p.shapes||p.d.map(d=>`<path d="${d}"/>`).join('');
  const attr=p.solid?`fill="${color}"`:`fill="none" stroke="${color}" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"`;
  return `<svg width="${size}" height="${size}" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg" ${attr}>${body}</svg>`;
}
function textNode(screen,name){return screen.findOne(n=>n.type==='TEXT'&&n.name===name);}
function replace(screen,name,type,color,size=22){
  const old=textNode(screen,name); if(!old)return;
  const x=old.x+(old.width-size)/2,y=old.y+(old.height-size)/2;
  const node=figma.createNodeFromSvg(svg(type,C[color]||color,size));
  screen.appendChild(node); node.name=name.replace(' / icon','')+' / vector icon'; node.resize(size,size); node.x=x; node.y=y;
  created.push(node.id); removed.push(old.id); old.remove();
}
function add(screen,name,type,color,x,y,size){const node=figma.createNodeFromSvg(svg(type,C[color]||color,size));screen.appendChild(node);node.name=name;node.resize(size,size);node.x=x;node.y=y;created.push(node.id);}
const screens=page.children.filter(n=>n.type==='FRAME');
for(const s of screens){
  if(s.name==='01 · Splash'||s.name==='03 · Sign in') replace(s,'Brand mark / heart','heart','white',26);
  if(s.name==='04 · Home'){
    replace(s,'Settings icon','settings','ink',22);
    for(const [name,type,color] of [['Angry','flame','angry'],['Sad','drop','sad'],['Stressed','waves','stressed'],['Anxious','spiral','anxious'],['Lonely','moon','lonely'],['Happy','sun','happy']]) replace(s,name+' / icon',type,color,22);
    replace(s,'Just talk icon','chat','purple',22);
  }
  if(s.name==='05 · Angry intensity')replace(s,'Angry icon','flame','angry',44);
  if(['05 · Angry intensity','06 · Angry conversation','07 · Sad retry','08 · Crisis support','09 · Settings','10 · Privacy & Help'].includes(s.name))replace(s,'Back arrow','back','ink',24);
  if(['06 · Angry conversation','07 · Sad retry'].includes(s.name)){
    replace(s,'More menu dots','more','ink',23);
    const m=textNode(s,'Mood Angry / label')||textNode(s,'Mood Sad / label');
    if(m){const isAngry=s.name.includes('Angry');m.characters=isAngry?'Angry':'Sad';m.x+=13;m.resize(m.width-13,m.height);mutated.push(m.id);add(s,'Mood vector icon',isAngry?'flame':'drop',isAngry?'angry':'sad',m.x-16,m.y+1,16);}
  }
  if(['06 · Angry conversation','07 · Sad retry','08 · Crisis support'].includes(s.name)){
    replace(s,'Composer / mic icon','mic','muted',20);
    replace(s,'Composer / send icon','send','white',22);
  }
  if(s.name==='08 · Crisis support'){
    const chip=textNode(s,'Support chip / label');if(chip){chip.characters='Get human support';chip.x+=9;chip.resize(chip.width-9,chip.height);mutated.push(chip.id);add(s,'Support shield icon','shield','purple',112,65,17);}
    for(const nm of ['Call Tele-MANAS / label','Call emergency / label']){const t=textNode(s,nm);if(t){t.characters='Call';t.x+=8;t.resize(t.width-8,t.height);mutated.push(t.id);add(s,nm+' / phone vector','phone','purple',t.x-11,t.y+2,14);}}
  }
  if(s.name==='10 · Privacy & Help'){
    const h=textNode(s,'Support heading');if(h){h.characters='Need a real person right now?';h.x=69;h.resize(279,h.height);mutated.push(h.id);add(s,'Support phone icon','phone','green',40,134,23);}
    for(const [name,type,color] of [['Not a therapist','chat','purple'],['How your messages are processed','sync','sad'],['What we store','storage','stressed'],['Deleting your data','trash','angry'],['Finding human support','people','lonely']])replace(s,name+' icon',type,color,20);
  }
}
// Bind exact theme-token colours to reusable Figma variables.
const vars=await figma.variables.getLocalVariablesAsync('COLOR');
const vm=new Map(vars.map(v=>[v.name,v]));
const coll=(await figma.variables.getLocalVariableCollectionsAsync()).find(c=>c.name==='LET IT OUT / Color');
const lm=coll.modes.find(m=>m.name==='Light').modeId;
if(!vm.has('color/on-primary')){const v=figma.variables.createVariable('color/on-primary',coll,'COLOR');v.scopes=['TEXT_FILL'];v.setValueForMode(lm,{r:1,g:1,b:1,a:1});vm.set(v.name,v);mutated.push(v.id);}
const byColor={
  '#F8F7FF':'color/canvas','#FFFFFF':'color/surface','#26243D':'color/ink','#5E5B7A':'color/muted','#6A5AE0':'color/primary','#8B7CF6':'color/brand-purple-tint',
  '#FFF3EC':'mood/angry/surface','#FFD9C9':'mood/angry/bubble','#C9633F':'mood/angry/accent',
  '#EEF4FF':'mood/sad/surface','#DCE7FF':'mood/sad/bubble','#3F7FC9':'mood/sad/accent',
  '#ECFBF6':'mood/stressed/surface','#CFF2E6':'mood/stressed/bubble','#1F8C72':'mood/stressed/accent',
  '#EDF8FA':'mood/anxious/surface','#D4EEF3':'mood/anxious/bubble','#2A8BA3':'mood/anxious/accent',
  '#FFF0F5':'mood/lonely/surface','#FFDCE8':'mood/lonely/bubble','#C9507F':'mood/lonely/accent',
  '#FFFBE8':'mood/happy/surface','#FFF0B8':'mood/happy/bubble','#A67C00':'mood/happy/accent',
  '#F3F0FF':'mood/neutral/surface','#E6E1FF':'mood/neutral/bubble'
};
function hx(c){return '#'+[c.r,c.g,c.b].map(v=>Math.round(v*255).toString(16).padStart(2,'0')).join('').toUpperCase();}
let bindings=0;
for(const n of screens.flatMap(s=>[s,...s.findAll(()=>true)])){
  if(!('fills' in n)||!Array.isArray(n.fills)||!n.fills.length)continue;
  const f=n.fills[0];if(f.type!=='SOLID')continue;
  const hex=hx(f.color);
  const key=n.type==='TEXT'&&hex==='#FFFFFF'?'color/on-primary':byColor[hex];
  const v=vm.get(key);if(!v)continue;
  const p=figma.variables.setBoundVariableForPaint(f,'color',v);
  n.fills=[p,...n.fills.slice(1)];bindings++;mutated.push(n.id);
}
return {createdNodeIds:created,removedNodeIds:removed,mutatedNodeIds:mutated,iconCount:created.length,variableBindings:bindings,screenCount:screens.length};
