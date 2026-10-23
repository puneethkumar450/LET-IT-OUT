// LET IT OUT — editable Claude/PDF redesign, 390 × 844 Android artboards.
// Executed inside Figma's use_figma Plugin API; all UI is native/editable.
const page = figma.root.children.find(p => p.id === '10:88');
if (!page) throw new Error('Redesign page not found');
await figma.setCurrentPageAsync(page);
page.name = 'LET IT OUT · Claude redesign (editable)';
const changed = [page.id];
const created = [];
const fonts = [
  {family:'Fredoka',style:'SemiBold'},
  {family:'Nunito',style:'Regular'},
  {family:'Nunito',style:'SemiBold'},
  {family:'Nunito',style:'Bold'},
  {family:'Nunito',style:'ExtraBold'}
];
for (const font of fonts) await figma.loadFontAsync(font);
const H = {
  bg:'#F8F7FF', ink:'#26243D', muted:'#5E5B7A', primary:'#6A5AE0',
  purple:'#8B7CF6', white:'#FFFFFF', lavender:'#E6E1FF',
  angryBg:'#FFF3EC', angryBubble:'#FFD9C9', angry:'#C9633F',
  sadBg:'#EEF4FF', sadBubble:'#DCE7FF', sad:'#3F7FC9',
  stressedBg:'#ECFBF6', stressedBubble:'#CFF2E6', stressed:'#1F8C72',
  anxiousBg:'#EDF8FA', anxiousBubble:'#D4EEF3', anxious:'#2A8BA3',
  lonelyBg:'#FFF0F5', lonelyBubble:'#FFDCE8', lonely:'#C9507F',
  happyBg:'#FFFBE8', happyBubble:'#FFF0B8', happy:'#A67C00',
  neutralBg:'#F3F0FF', neutralBubble:'#E6E1FF', neutral:'#6A5AE0',
  green:'#238D7C', line:'#E5E1F0', danger:'#C9633F'
};
const rgb = hex => { const n=parseInt(hex.slice(1),16); return {r:((n>>16)&255)/255,g:((n>>8)&255)/255,b:(n&255)/255}; };
const colors = await figma.variables.getLocalVariablesAsync('COLOR');
const collection = (await figma.variables.getLocalVariableCollectionsAsync()).find(c=>c.name==='LET IT OUT / Color');
const lm = collection.modes.find(m=>m.name==='Light').modeId;
const varMap = new Map(colors.map(v=>[v.name,v]));
function variable(name, hex, scopes) {
  let v=varMap.get(name);
  if (!v) { v=figma.variables.createVariable(name,collection,'COLOR'); varMap.set(name,v); }
  v.scopes=scopes;
  v.setValueForMode(lm,{...rgb(hex),a:1});
  v.setVariableCodeSyntax('ANDROID',name.replaceAll('/','_').replaceAll('-','_'));
  changed.push(v.id);
  return v;
}
variable('color/canvas',H.bg,['FRAME_FILL','SHAPE_FILL']);
variable('color/surface',H.white,['FRAME_FILL','SHAPE_FILL']);
variable('color/ink',H.ink,['TEXT_FILL']);
variable('color/muted',H.muted,['TEXT_FILL']);
variable('color/primary',H.primary,['FRAME_FILL','SHAPE_FILL','TEXT_FILL']);
variable('color/brand-purple-tint',H.purple,['FRAME_FILL','SHAPE_FILL','TEXT_FILL']);
// This Starter-plan file permits one mode, so expose dark values as
// separately named tokens for handoff instead of adding a second mode.
variable('dark/surface','#17162A',['FRAME_FILL','SHAPE_FILL']);
variable('dark/text','#F1EFFF',['TEXT_FILL']);
for (const mood of ['angry','sad','stressed','anxious','lonely','happy','neutral']) {
  variable('mood/'+mood+'/surface',H[mood+'Bg'],['FRAME_FILL','SHAPE_FILL']);
  variable('mood/'+mood+'/bubble',H[mood+'Bubble'],['FRAME_FILL','SHAPE_FILL']);
  variable('mood/'+mood+'/accent',H[mood],['FRAME_FILL','SHAPE_FILL','TEXT_FILL']);
}
function paint(c) { return {type:'SOLID',color:rgb(H[c] || c)}; }
function put(n,p,name,x,y,w,h) { p.appendChild(n); n.name=name; n.x=x; n.y=y; n.resize(w,h); created.push(n.id); return n; }
function frame(p,name,x,y,w,h,c,r=0) { const n=put(figma.createFrame(),p,name,x,y,w,h); n.fills=c?[paint(c)]:[]; n.cornerRadius=r; n.clipsContent=false; return n; }
function box(p,name,x,y,w,h,c,r=0,stroke=null,sw=1) { const n=put(figma.createRectangle(),p,name,x,y,w,h); n.fills=c?[paint(c)]:[]; n.cornerRadius=r; if(stroke){n.strokes=[paint(stroke)];n.strokeWeight=sw;} return n; }
function oval(p,name,x,y,w,h,c) { const n=put(figma.createEllipse(),p,name,x,y,w,h); n.fills=[paint(c)]; return n; }
function txt(p,name,s,x,y,w,size=16,c='ink',weight='Regular',align='LEFT',lh=null) {
  const n=figma.createText(); p.appendChild(n); n.name=name; n.x=x; n.y=y;
  n.fontName={family:weight==='Display'?'Fredoka':'Nunito',style:weight==='Display'?'SemiBold':weight};
  n.fontSize=size; n.fills=[paint(c)]; n.textAlignHorizontal=align;
  n.resize(w,Math.max(24,size*1.5)); n.textAutoResize='HEIGHT';
  n.lineHeight={unit:'PIXELS',value:lh||Math.round(size*1.35)};
  n.characters=s; created.push(n.id); return n;
}
function centered(p,name,s,x,y,w,size,c='ink',weight='Regular',lh=null) { return txt(p,name,s,x,y,w,size,c,weight,'CENTER',lh); }
function cta(p,name,label,x,y,w=342,c='primary') { box(p,name+' / background',x,y,w,56,c,28); centered(p,name+' / label',label,x,y+16,w,16,'white','Bold'); }
function iconChip(p,name,label,x,y,w,h,c='white',tc='ink') { box(p,name+' / background',x,y,w,h,c,Math.min(h/2,18)); centered(p,name+' / label',label,x,y+(h-20)/2,w,14,tc,'Bold'); }
function back(p) { box(p,'Back button',18,50,48,48,'white',16); centered(p,'Back arrow','‹',18,52,48,31,'ink','Regular'); }
function handle(p) { box(p,'Gesture handle',142,829,106,4,'#26243D',2); }
function makeScreen(name,x,y,bg='bg') { const n=frame(page,name,x,y,390,844,bg); n.clipsContent=true; return n; }
function brand(p,x,y,scale=1) {
  const s=112*scale;
  box(p,'Brand mark / white tile',x,y,s,s,'white',30*scale);
  box(p,'Brand mark / speech bubble',x+25*scale,y+35*scale,62*scale,45*scale,'purple',9*scale);
  // Editable heart glyph in the bubble.
  centered(p,'Brand mark / heart','♥',x+28*scale,y+39*scale,56*scale,36*scale,'white','Bold');
}
function moodTile(p,name,x,y,bg,accent,symbol) {
  box(p,name+' / tile',x,y,164,132,bg,22);
  oval(p,name+' / badge',x+16,y+15,40,40,'white');
  centered(p,name+' / icon',symbol,x+22,y+19,28,22,accent,'Bold');
  txt(p,name+' / label',name,x+16,y+93,132,17,'ink','Bold');
}
function bubble(p,name,s,x,y,w,h,c,tc='ink',fs=15) {
  box(p,name+' / fill',x,y,w,h,c,22);
  txt(p,name+' / text',s,x+16,y+12,w-32,fs,tc,'Regular','LEFT',Math.round(fs*1.35));
}
function composer(p,accent='primary') {
  box(p,'Composer / input',18,762,354,58,'white',29);
  txt(p,'Composer / hint',"Say what's on your mind...",36,781,213,14,'muted');
  oval(p,'Composer / microphone',278,770,42,42,'#F0EFF8');
  centered(p,'Composer / mic icon','♩',282,778,34,21,'muted','Bold');
  oval(p,'Composer / send',325,770,42,42,accent);
  centered(p,'Composer / send icon','↑',329,774,34,27,'white','Bold');
}
function settingRow(p,label,value,y,opts={}) {
  txt(p,label+' / label',label,38,y+17,262,15,opts.danger?'angry':'ink',opts.danger?'Bold':'Regular');
  if(value) txt(p,label+' / value',value,217,y+17,130,14,opts.valueColor||'primary','Bold','RIGHT');
  if(!opts.noArrow) centered(p,label+' / chevron','›',348,y+13,19,22,'muted');
}

// 01 Splash
{
  const p=makeScreen('01 · Splash',80,80);
  oval(p,'Lavender corner orb',-94,-114,342,342,'lavender');
  oval(p,'Peach side orb',315,310,164,164,'angryBubble');
  oval(p,'Mint bottom orb',130,632,365,365,'stressedBubble');
  brand(p,139,307,1);
  centered(p,'Wordmark','LET IT OUT',34,454,322,39,'ink','Display');
  centered(p,'Tagline','Your Feelings Matter.',40,509,310,18,'muted','SemiBold');
  centered(p,'Scope disclosure','An AI listening companion — not a therapist or a\nsubstitute for professional care.',36,770,318,12,'muted','Regular',18);
}

// 02 Onboarding
{
  const p=makeScreen('02 · Onboarding',550,80);
  txt(p,'Skip','Skip',324,60,42,15,'muted','Bold','RIGHT');
  oval(p,'Illustration lavender orb',42,178,300,300,'lavender');
  oval(p,'Illustration peach orb',232,162,148,148,'#FFE2D4');
  oval(p,'Illustration mint orb',18,388,108,108,'stressedBubble');
  bubble(p,'Example user message',"Today was a lot. I don't even\nknow where to start.",77,270,233,74,'white','ink',14);
  bubble(p,'Example AI reply',"That's okay. Start anywhere\n— I'm listening.",77,353,234,75,'primary','white',14);
  centered(p,'Onboarding title','A Space to Express\nYourself',30,529,330,29,'ink','Display',33);
  centered(p,'Onboarding subtitle','Talk about whatever is on your mind.',34,616,322,16,'muted');
  box(p,'Active page dot',167,666,24,8,'primary',4);
  oval(p,'Page dot 2',202,666,8,8,'line'); oval(p,'Page dot 3',218,666,8,8,'line');
  cta(p,'Get Started','Get Started',24,698);
  centered(p,'AI disclosure',"You'll be talking with an AI, not a person. Messages are\nprocessed to generate replies and aren't stored unless you\nchoose to save them. For adults 18+.",24,765,342,11,'muted','Regular',16);
}

// 03 Sign in
{
  const p=makeScreen('03 · Sign in',1020,80);
  oval(p,'Top lavender orb',244,-88,250,250,'lavender');
  brand(p,26,74,.5);
  txt(p,'Welcome heading','Welcome',26,192,338,32,'ink','Display');
  txt(p,'Sign in subtitle','Sign in to keep your preferences across\ndevices, or just start talking as a guest.',26,238,338,16,'muted','Regular','LEFT',24);
  box(p,'Google button',24,354,342,56,'white',28,'line',1.5);
  oval(p,'Google badge',87,368,28,28,'white');
  centered(p,'Google G','G',89,370,24,18,'primary','Display');
  centered(p,'Google button label','Continue with Google',122,372,190,16,'ink','Bold');
  cta(p,'Guest button','Continue as guest',24,424);
  centered(p,'Guest note','Guest mode needs no account. You can link Google\nlater in Settings and keep your preferences.',28,501,334,13,'muted','Regular',19);
  box(p,'Trust card',24,679,342,93,'white',20);
  centered(p,'Trust shield','♢',42,694,31,26,'green','Bold');
  txt(p,'Trust message','We never see your Google password. Sign-in\nonly shares your name and email with\nLet It Out.',76,694,270,13,'ink','Regular','LEFT',18);
  centered(p,'Terms and privacy','By continuing you agree to the Terms and Privacy Policy.\nLet It Out is for adults 18 and older.',27,790,336,11,'muted','Regular',16);
}

// 04 Home
{
  const p=makeScreen('04 · Home',1490,80);
  txt(p,'Greeting','Hello there 💙',24,58,250,17,'muted','Bold');
  txt(p,'Prompt','How are you feeling\ntoday?',24,91,300,30,'ink','Display','LEFT',34);
  box(p,'Settings button',329,55,42,42,'white',14); centered(p,'Settings icon','⚙',333,63,34,22,'ink','Regular');
  moodTile(p,'Angry',24,174,'#FFE3D7','angry','♨');
  moodTile(p,'Sad',202,174,'#DDEBFF','sad','♧');
  moodTile(p,'Stressed',24,318,'#D9F5EA','stressed','≈');
  moodTile(p,'Anxious',202,318,'#D8F2F6','anxious','◔');
  moodTile(p,'Lonely',24,462,'#FFE0EC','lonely','☾');
  moodTile(p,'Happy',202,462,'#FFF3BE','happy','☼');
  box(p,'Just talk tile',24,606,342,74,'lavender',22);
  oval(p,'Just talk badge',40,620,44,44,'white'); centered(p,'Just talk icon','▢',47,630,30,20,'primary','Bold');
  txt(p,'Just talk label','Just want to talk',96,617,240,16,'ink','Bold');
  txt(p,'Just talk note','No label needed — start anywhere',96,641,250,13,'muted');
  cta(p,'Lets talk',"Let's Talk",24,733);
  centered(p,'New conversation','Start a new conversation',24,805,342,14,'primary','Bold');
}

// 05 Angry intensity
{
  const p=makeScreen('05 · Angry intensity',1960,80,'angryBg');
  back(p); iconChip(p,'Change mood','Change mood',252,56,114,38,'white','angry');
  box(p,'Feeling card',24,129,342,280,'angryBubble',28);
  oval(p,'Feeling card pale orb',269,87,178,178,'#FFF0DE');
  oval(p,'Angry icon circle',151,167,88,88,'white');
  centered(p,'Angry icon','♨',168,188,54,44,'angry','Bold');
  centered(p,'Feeling heading','Feeling angry',42,280,306,29,'ink','Display');
  centered(p,'Feeling reassurance',"It's okay to feel this way. You can say it\nhowever it comes out — no judgment\nhere.",48,324,294,15,'muted','Regular',22);
  box(p,'Intensity card',24,433,342,204,'white',24);
  txt(p,'Intensity heading','How intense is this feeling?',44,455,259,17,'ink','Bold');
  txt(p,'Optional','Optional',291,459,55,12,'muted','Bold','RIGHT');
  box(p,'Slider track',45,512,298,5,'#E2DFE0',3);
  box(p,'Slider active',45,512,190,5,'angry',3);
  oval(p,'Slider thumb',229,505,18,18,'angry');
  txt(p,'Slider low','1 · A little',45,550,112,13,'muted','SemiBold');
  centered(p,'Slider value','7',179,550,32,13,'angry','Bold');
  txt(p,'Slider high','10 · A lot',262,550,82,13,'muted','SemiBold','RIGHT');
  txt(p,'Intensity note',"This just helps the conversation start in the right\nplace. It isn't a measurement of anything.",44,584,302,12,'muted','Regular','LEFT',18);
  cta(p,'Start talking','Start talking',24,733,342,'angry');
  centered(p,'Skip rating','Skip the rating',24,805,342,14,'angry','Bold');
}

// 06 Angry conversation
{
  const p=makeScreen('06 · Angry conversation',80,1024,'angryBg');
  back(p); iconChip(p,'Mood Angry','♨  Angry',145,57,100,37,'angryBubble','ink');
  box(p,'More menu button',324,50,48,48,'white',16); centered(p,'More menu dots','···',328,54,40,27,'ink','Bold');
  centered(p,'Conversation ephemerality','Today · Nothing here is saved',42,194,306,11,'muted','SemiBold');
  bubble(p,'User message 1',"I'm so angry! Everyone is irritating me!",70,222,295,52,'angryBubble');
  bubble(p,'AI message 1','That sounds really frustrating.\nWhat happened?',45,281,252,74,'white');
  bubble(p,'User message 2','My manager insulted me in front of the\nwhole team!',54,363,311,73,'angryBubble');
  bubble(p,'AI message 2','That must have been difficult.\nDo you want to tell me what\nhappened?',45,444,273,92,'white');
  bubble(p,'User message 3','I hate everything today!',191,544,174,52,'angryBubble');
  bubble(p,'AI message 3',"It sounds like a lot has built up\ntoday. I'm listening.",45,604,288,115,'white');
  box(p,'Voice separator',60,673,258,1,'#F4E6E0');
  txt(p,'Speaking status','▥ Speaking...',61,684,136,13,'angry','Bold');
  iconChip(p,'Stop speaking','■  Stop',243,678,73,33,'white','angry');
  composer(p,'angry');
}

// 07 Sad conversation with retry
{
  const p=makeScreen('07 · Sad retry',550,1024,'sadBg');
  back(p); iconChip(p,'Mood Sad','♧  Sad',149,57,88,37,'sadBubble','ink');
  box(p,'More menu button',324,50,48,48,'white',16); centered(p,'More menu dots','···',328,54,40,27,'ink','Bold');
  bubble(p,'AI message 1',"I'm here. Whenever you're\nready, what's been on your\nmind?",56,184,258,93,'white');
  bubble(p,'User message 1',"I don't know. Just feeling really low\nlately.",69,284,296,72,'sadBubble');
  bubble(p,'AI message 2','That sounds heavy. Has\nsomething changed recently, or\nhas it been building for a while?',56,364,273,93,'white');
  bubble(p,'User message 2','My best friend moved away last month\nand everything feels empty.',69,465,296,72,'sadBubble');
  box(p,'Retry card',50,547,294,139,'white',20,'#BED5F0',1.5);
  txt(p,'Retry message',"I couldn't reach the server just\nnow. Your message is still here\n— nothing was lost.",69,559,253,14,'ink','Regular','LEFT',19);
  iconChip(p,'Try again','Try again',69,630,97,37,'sad','white');
  iconChip(p,'Edit message','Edit message',173,630,119,37,'sadBg','sad');
  iconChip(p,'Offline status','●  No internet connection',113,710,189,29,'white','muted');
  composer(p,'sad');
}

// 08 Crisis support
{
  const p=makeScreen('08 · Crisis support',1020,1024,'neutralBg');
  back(p); iconChip(p,'Support chip','◇  Get human support',99,56,190,38,'white','primary');
  txt(p,'Leave action','Leave',323,66,48,15,'muted','SemiBold','RIGHT');
  bubble(p,'User distress',"I'm so tired of everything. I don't know\nhow to keep going.",61,242,304,73,'lavender');
  box(p,'AI response',50,325,312,405,'white',23);
  txt(p,'AI support reply',"It sounds like you're carrying something\nreally heavy right now, and I'm glad you\nsaid it here. I'm an AI, so I can't be with\nyou the way a person can — but there\nare people who can, right now.",67,343,280,15,'ink','Regular','LEFT',21);
  box(p,'Tele-MANAS card',67,480,280,76,'neutralBg',16);
  txt(p,'Tele-MANAS title','Tele-MANAS · 14416',81,491,165,14,'ink','Bold');
  txt(p,'Tele-MANAS detail','Free, 24/7 mental health\nsupport (India)',81,512,162,12,'muted','Regular','LEFT',16);
  iconChip(p,'Call Tele-MANAS','♧  Call',259,499,78,43,'primary','white');
  box(p,'Emergency card',67,563,280,75,'neutralBg',16);
  txt(p,'Emergency title','Emergency · 112',81,574,165,14,'ink','Bold');
  txt(p,'Emergency detail',"If you're in immediate\ndanger",81,595,162,12,'muted','Regular','LEFT',16);
  iconChip(p,'Call emergency','♧  Call',259,582,78,42,'white','primary');
  txt(p,'AI limitation',"Let It Out can't call anyone or alert a person for\nyou. These numbers go to real people. I'm still\nhere if you want to keep talking.",67,652,280,12,'muted','Regular','LEFT',17);
  composer(p,'primary');
}

// 09 Settings
{
  const p=makeScreen('09 · Settings',1490,1024);
  back(p); txt(p,'Settings title','Settings',76,60,275,26,'ink','Display');
  box(p,'Guest card',24,114,342,86,'white',22);
  oval(p,'Guest avatar',39,132,49,49,'lavender'); centered(p,'Guest avatar G','G',51,142,25,20,'primary','Display');
  txt(p,'Guest title','Guest',101,129,210,16,'ink','Bold');
  txt(p,'Guest detail','Link Google to keep your\npreferences across devices',101,151,190,12,'muted','Regular','LEFT',16);
  iconChip(p,'Link account','Link',303,136,48,35,'primary','white');
  txt(p,'Appearance section','Appearance',27,218,230,12,'muted','Bold');
  box(p,'Appearance group',24,240,342,55,'white',18); settingRow(p,'App theme','Follow my mood',240);
  txt(p,'Speech section','Speech',27,313,230,12,'muted','Bold');
  box(p,'Speech group',24,335,342,100,'white',18);
  settingRow(p,'Read replies aloud','',335,{noArrow:true});
  box(p,'Speech divider',43,385,304,1,'line');
  settingRow(p,'Voice input (push to talk)','',385,{noArrow:true});
  for (const y of [350,400]) { box(p,'Toggle track '+y,317,y,37,22,'primary',11); oval(p,'Toggle thumb '+y,335,y+3,16,16,'white'); }
  txt(p,'Privacy section','Privacy & data',27,455,230,12,'muted','Bold');
  box(p,'Privacy group',24,477,342,149,'white',18);
  settingRow(p,'Privacy policy','',477); box(p,'Privacy divider 1',42,526,306,1,'line');
  settingRow(p,'Terms of use','',527); box(p,'Privacy divider 2',42,576,306,1,'line');
  settingRow(p,'Delete conversation data on this device','',577);
  txt(p,'Account section','Account',27,646,230,12,'muted','Bold');
  box(p,'Account group',24,668,342,101,'white',18);
  settingRow(p,'Sign out','',668,{noArrow:true}); box(p,'Account divider',42,718,306,1,'line');
  settingRow(p,'Delete account and data','',719,{danger:true,noArrow:true});
  txt(p,'About link','About Let It Out',27,807,200,13,'primary','Bold');
  txt(p,'Version','Version 0.1.0',265,807,100,12,'muted','Regular','RIGHT');
}

// 10 Privacy & Help
{
  const p=makeScreen('10 · Privacy & Help',1960,1024);
  back(p); txt(p,'Privacy title','Privacy & Help',76,60,290,25,'ink','Display');
  box(p,'Human support card',24,114,342,229,'stressedBubble',23);
  txt(p,'Support heading','♧  Need a real person right now?',40,132,310,18,'ink','Display');
  txt(p,'Support subtitle',"Let It Out is an AI and can't help in an emergency.\nThese lines are answered by people.",40,171,310,13,'ink','Regular','LEFT',19);
  box(p,'Tele-MANAS helpline',40,224,147,93,'green',16);
  txt(p,'Tele-MANAS helpline title','Tele-MANAS\n14416',53,236,120,16,'white','Bold','LEFT',18);
  txt(p,'Tele-MANAS helpline detail','Mental health · free ·\n24/7',53,281,119,11,'white','Regular','LEFT',14);
  box(p,'Emergency helpline',197,224,153,93,'white',16,'green',1.5);
  txt(p,'Emergency helpline title','Emergency 112',209,240,128,15,'ink','Bold');
  txt(p,'Emergency helpline detail','Immediate danger',209,270,125,11,'muted');
  box(p,'Help topic list',24,358,342,378,'white',20);
  const topics=[
    ['Not a therapist','An AI companion, not medical care or\ndiagnosis','lavender','primary','▢'],
    ['How your messages are processed','Sent to our server and an AI provider to\nreply','sadBubble','sad','◌'],
    ['What we store','Mood, settings on your device — not\nconversations','stressedBubble','stressed','▣'],
    ['Deleting your data','Clear a chat, wipe this device, or delete\nyour account','angryBubble','angry','⌫'],
    ['Finding human support','Helplines, counsellors and when to reach\nout','lonelyBubble','lonely','♧']
  ];
  topics.forEach((a,i)=>{
    const y=365+i*74;
    if(i) box(p,'Topic divider '+i,40,y-3,310,1,'#F0EEF4');
    box(p,a[0]+' icon tile',39,y+8,38,38,a[2],11);
    centered(p,a[0]+' icon',a[4],43,y+15,30,21,a[3],'Bold');
    txt(p,a[0]+' title',a[0],89,y+5,247,14,'ink','Bold');
    txt(p,a[0]+' summary',a[1],89,y+27,247,11,'muted','Regular','LEFT',15);
    centered(p,a[0]+' chevron','›',345,y+20,16,21,'muted');
  });
  txt(p,'Safety footnote','Let It Out does not diagnose, treat or cure any condition.\nHelpline numbers are for India and are checked before each\nrelease.',30,771,330,11,'muted','Regular','LEFT',16);
}

figma.viewport.scrollAndZoomIntoView(page.children.filter(n=>n.type==='FRAME'));
return {createdNodeIds:created,mutatedNodeIds:changed,screenCount:page.children.filter(n=>n.type==='FRAME').length,screens:page.children.filter(n=>n.type==='FRAME').map(n=>({id:n.id,name:n.name,x:n.x,y:n.y,width:n.width,height:n.height,descendants:n.findAll(()=>true).length})),variableCount:varMap.size};
