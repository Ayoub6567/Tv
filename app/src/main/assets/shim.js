/* طبقة اتصال أصلية: تستبدل الوسيط بطلبات مباشرة من الهاتف (بدون Cloudflare) */
(function(){
const UA="Mozilla/5.0 (QtEmbedded; U; Linux; C) AppleWebKit/533.3 (KHTML, like Gecko) MAG200 stbapp ver: 2 rev: 250 Safari/533.3";
let nid=0;const pend={},SESS={};
window.__nf=(id,st,txt)=>{const r=pend[id];if(r){delete pend[id];r({status:st,text:txt})}};
const nf=(url,h)=>new Promise(res=>{const id=++nid;pend[id]=res;Native.fetch(id,url,JSON.stringify(h||{}))});
const sha=t=>Native.sha(t);
function pp(p){const u=new URL(/^https?:\/\//i.test(p)?p:"http://"+p);return{origin:u.origin,prefix:u.pathname.replace(/\/+$/,"").replace(/\/c$/i,"")}}
function hd(mac,tk,portal,enc){const h={"User-Agent":UA,"X-User-Agent":"Model: MAG250; Link: WiFi","Accept":"*/*","Accept-Language":"en-US,*","Cookie":"mac="+(enc?encodeURIComponent(mac):mac)+"; stb_lang=en; timezone=GMT","Referer":portal.replace(/\/+$/,"")+"/"};if(tk)h.Authorization="Bearer "+tk;return h}
async function gj(url,h){const r=await nf(url,h);if(r.status===0)throw new Error("تعذّر الاتصال: "+r.text.replace(/^ERR:/,""));let j=null;try{j=JSON.parse(r.text)}catch(e){const m=r.text.match(/\{[\s\S]*\}/);if(m)try{j=JSON.parse(m[0])}catch(x){}}return{status:r.status,text:r.text,json:j}}
async function session(portal,mac,force){const key=portal+"|"+mac;let s=SESS[key];if(s&&!force&&Date.now()-s.ts<12e5)return s;
 const{origin,prefix}=pp(portal);const c=[`${origin}${prefix}/server/load.php`,`${origin}${prefix}/portal.php`,`${origin}${prefix}/stalker_portal/server/load.php`,`${origin}${prefix}/c/server/load.php`];if(s&&s.load)c.unshift(s.load);
 const tried=[];
 for(const load of [...new Set(c)])for(const enc of [false,true]){
  try{const r=await gj(`${load}?type=stb&action=handshake&token=&prehash=0&JsHttpRequest=1-xml`,hd(mac,"",portal,enc));const tk=r.json&&r.json.js&&r.json.js.token;
   if(tk){const sn=sha(mac).slice(0,13).toUpperCase();
    const q=new URLSearchParams({type:"stb",action:"get_profile",hd:"1",ver:"ImageDescription: 0.2.18-r14-pub-250; ImageDate: Fri Jan 15 15:20:44 EET 2016; PORTAL version: 5.6.1; API Version: JS API version: 328; STB API version: 134; Player Version: 0x566",num_banks:"2",sn,stb_type:"MAG250",client_type:"STB",image_version:"218",video_out:"hdmi",device_id:sha(mac+"1").toUpperCase(),device_id2:sha(mac+"2").toUpperCase(),signature:"",auth_second_step:"1",hw_version:"1.7-BD-00",not_valid_token:"0",metrics:JSON.stringify({mac,sn,type:"STB",model:"MAG250",uid:"",random:""}),hw_version_2:sha(mac+"3").slice(0,32),timestamp:String(Math.floor(Date.now()/1000)),api_signature:"262",prehash:"",JsHttpRequest:"1-xml"});
    await gj(`${load}?${q}`,hd(mac,tk,portal,enc));
    return SESS[key]={token:tk,load,enc,ts:Date.now()}}
   tried.push(load+" → "+r.status+" "+r.text.slice(0,60).replace(/\s+/g," "))}catch(e){tried.push(load+" → "+e.message)}}
 throw new Error("فشل تسجيل الدخول إلى البوابة — "+tried.join(" | ").slice(0,300))}
async function stalker(portal,mac,params){let s=await session(portal,mac,false),last=null;
 for(let a=0;a<2;a++){const q=new URLSearchParams(params);q.set("JsHttpRequest","1-xml");last=await gj(`${s.load}?${q}`,hd(mac,s.token,portal,s.enc));
  if(last.json&&last.status<400&&!/Authorization failed|ACCESS_DENIED|not authorized/i.test(last.text.slice(0,300)))return last;
  if(a===0)s=await session(portal,mac,true)}
 return last}
window.api=async function(params){const P=prof(),r=await stalker(P.p,P.m.toUpperCase(),params);
 try{mkLog(params,r.status+" "+r.text)}catch(e){}
 if(!r.json)throw new Error("رد غير مفهوم من البوابة (HTTP "+r.status+")");
 return r.json.js===undefined?r.json:r.json.js};
window.plOpen=async function(list,n){const c=list[n];if(!c)return;
 try{toast("جارٍ تجهيز البث...");const u=await resolveLink(c);if(!c.vod&&c.it)addRec(c.it);
  Native.play(u,c.n+(c.vod&&c.sub?" — "+c.sub:""),prof().m.toUpperCase(),!c.vod)}catch(e){toast(e.message||String(e),4000)}};
})();
