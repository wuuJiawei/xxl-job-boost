import{$ as e,C as t,E as n,H as r,P as i,St as a,W as o,_ as s,b as c,d as l,g as u,k as d,ut as f,v as p,vt as m,w as h,y as g}from"./vue.runtime.esm-bundler-CoGLJnd5.js";import{$t as _,Bt as v,Gt as y,Jt as b,Kt as x,Q as S,Qt as C,Xt as w,Yt as T,Z as E,Zt as D,at as O,f as k,g as A,m as j,r as M,rt as N,v as P,vt as F,x as I,yt as L}from"./axios-BwS9P_IR.js";import{t as R}from"./Input-ZK5LI6Wg.js";import{d as z,t as B}from"./Button-BoJU_Zkc.js";import{i as V,t as H}from"./FormItem-B7uOGGUo.js";import{t as U}from"./Checkbox-qsyEeADO.js";import{$ as W,H as G,M as K,et as q,it as J,nt as Y,rt as X,tt as Z}from"./index-CGPgfFts.js";import{t as Q}from"./http-BDWJ50mN.js";function $(e){let{lineHeight:t,borderRadius:n,fontWeightStrong:r,baseColor:i,dividerColor:a,actionColor:o,textColor1:s,textColor2:c,closeColorHover:l,closeColorPressed:u,closeIconColor:d,closeIconColorHover:f,closeIconColorPressed:p,infoColor:m,successColor:h,warningColor:g,errorColor:_,fontSize:v}=e;return Object.assign(Object.assign({},q),{fontSize:v,lineHeight:t,titleFontWeight:r,borderRadius:n,border:`1px solid ${a}`,color:o,titleTextColor:s,iconColor:c,contentTextColor:c,closeBorderRadius:n,closeColorHover:l,closeColorPressed:u,closeIconColor:d,closeIconColorHover:f,closeIconColorPressed:p,borderInfo:`1px solid ${L(i,F(m,{alpha:.25}))}`,colorInfo:L(i,F(m,{alpha:.08})),titleTextColorInfo:s,iconColorInfo:m,contentTextColorInfo:c,closeColorHoverInfo:l,closeColorPressedInfo:u,closeIconColorInfo:d,closeIconColorHoverInfo:f,closeIconColorPressedInfo:p,borderSuccess:`1px solid ${L(i,F(h,{alpha:.25}))}`,colorSuccess:L(i,F(h,{alpha:.08})),titleTextColorSuccess:s,iconColorSuccess:h,contentTextColorSuccess:c,closeColorHoverSuccess:l,closeColorPressedSuccess:u,closeIconColorSuccess:d,closeIconColorHoverSuccess:f,closeIconColorPressedSuccess:p,borderWarning:`1px solid ${L(i,F(g,{alpha:.33}))}`,colorWarning:L(i,F(g,{alpha:.08})),titleTextColorWarning:s,iconColorWarning:g,contentTextColorWarning:c,closeColorHoverWarning:l,closeColorPressedWarning:u,closeIconColorWarning:d,closeIconColorHoverWarning:f,closeIconColorPressedWarning:p,borderError:`1px solid ${L(i,F(_,{alpha:.25}))}`,colorError:L(i,F(_,{alpha:.08})),titleTextColorError:s,iconColorError:_,contentTextColorError:c,closeColorHoverError:l,closeColorPressedError:u,closeIconColorError:d,closeIconColorHoverError:f,closeIconColorPressedError:p})}var ee={name:`Alert`,common:k,self:$},te=x(`alert`,`
 line-height: var(--n-line-height);
 border-radius: var(--n-border-radius);
 position: relative;
 transition: background-color .3s var(--n-bezier);
 background-color: var(--n-color);
 text-align: start;
 word-break: break-word;
`,[b(`border`,`
 border-radius: inherit;
 position: absolute;
 left: 0;
 right: 0;
 top: 0;
 bottom: 0;
 transition: border-color .3s var(--n-bezier);
 border: var(--n-border);
 pointer-events: none;
 `),T(`closable`,[x(`alert-body`,[b(`title`,`
 padding-right: 24px;
 `)])]),b(`icon`,{color:`var(--n-icon-color)`}),x(`alert-body`,{padding:`var(--n-padding)`},[b(`title`,{color:`var(--n-title-text-color)`}),b(`content`,{color:`var(--n-content-text-color)`})]),W({originalTransition:`transform .3s var(--n-bezier)`,enterToProps:{transform:`scale(1)`},leaveToProps:{transform:`scale(0.9)`}}),b(`icon`,`
 position: absolute;
 left: 0;
 top: 0;
 align-items: center;
 justify-content: center;
 display: flex;
 width: var(--n-icon-size);
 height: var(--n-icon-size);
 font-size: var(--n-icon-size);
 margin: var(--n-icon-margin);
 `),b(`close`,`
 transition:
 color .3s var(--n-bezier),
 background-color .3s var(--n-bezier);
 position: absolute;
 right: 0;
 top: 0;
 margin: var(--n-close-margin);
 `),T(`show-icon`,[x(`alert-body`,{paddingLeft:`calc(var(--n-icon-margin-left) + var(--n-icon-size) + var(--n-icon-margin-right))`})]),T(`right-adjust`,[x(`alert-body`,{paddingRight:`calc(var(--n-close-size) + var(--n-padding) + 2px)`})]),x(`alert-body`,`
 border-radius: var(--n-border-radius);
 transition: border-color .3s var(--n-bezier);
 `,[b(`title`,`
 transition: color .3s var(--n-bezier);
 font-size: 16px;
 line-height: 19px;
 font-weight: var(--n-title-font-weight);
 `,[y(`& +`,[b(`content`,{marginTop:`9px`})])]),b(`content`,{transition:`color .3s var(--n-bezier)`,fontSize:`var(--n-font-size)`})]),b(`icon`,{transition:`color .3s var(--n-bezier)`})]),ne=n({name:`Alert`,inheritAttrs:!1,props:Object.assign(Object.assign({},P.props),{title:String,showIcon:{type:Boolean,default:!0},type:{type:String,default:`default`},bordered:{type:Boolean,default:!0},closable:Boolean,onClose:Function,onAfterLeave:Function,onAfterHide:Function}),slots:Object,setup(e){let{mergedClsPrefixRef:t,mergedBorderedRef:n,inlineThemeDisabled:r,mergedRtlRef:i}=S(e),a=P(`Alert`,`-alert`,te,ee,e,t),o=I(`Alert`,i,t),s=u(()=>{let{common:{cubicBezierEaseInOut:t},self:n}=a.value,{fontSize:r,borderRadius:i,titleFontWeight:o,lineHeight:s,iconSize:c,iconMargin:l,iconMarginRtl:u,closeIconSize:d,closeBorderRadius:f,closeSize:p,closeMargin:m,closeMarginRtl:h,padding:g}=n,{type:_}=e,{left:y,right:b}=v(l);return{"--n-bezier":t,"--n-color":n[D(`color`,_)],"--n-close-icon-size":d,"--n-close-border-radius":f,"--n-close-color-hover":n[D(`closeColorHover`,_)],"--n-close-color-pressed":n[D(`closeColorPressed`,_)],"--n-close-icon-color":n[D(`closeIconColor`,_)],"--n-close-icon-color-hover":n[D(`closeIconColorHover`,_)],"--n-close-icon-color-pressed":n[D(`closeIconColorPressed`,_)],"--n-icon-color":n[D(`iconColor`,_)],"--n-border":n[D(`border`,_)],"--n-title-text-color":n[D(`titleTextColor`,_)],"--n-content-text-color":n[D(`contentTextColor`,_)],"--n-line-height":s,"--n-border-radius":i,"--n-font-size":r,"--n-title-font-weight":o,"--n-icon-size":c,"--n-icon-margin":l,"--n-icon-margin-rtl":u,"--n-close-size":p,"--n-close-margin":m,"--n-close-margin-rtl":h,"--n-padding":g,"--n-icon-margin-left":y,"--n-icon-margin-right":b}}),c=r?E(`alert`,u(()=>e.type[0]),s,e):void 0,l=f(!0),d=()=>{let{onAfterLeave:t,onAfterHide:n}=e;t&&t(),n&&n()};return{rtlEnabled:o,mergedClsPrefix:t,mergedBordered:n,visible:l,handleCloseClick:()=>{Promise.resolve(e.onClose?.call(e)).then(e=>{e!==!1&&(l.value=!1)})},handleAfterLeave:()=>{d()},mergedTheme:a,cssVars:r?void 0:s,themeClass:c?.themeClass,onRender:c?.onRender}},render(){var e;return(e=this.onRender)==null||e.call(this),d(z,{onAfterLeave:this.handleAfterLeave},{default:()=>{let{mergedClsPrefix:e,$slots:t}=this,n={class:[`${e}-alert`,this.themeClass,this.closable&&`${e}-alert--closable`,this.showIcon&&`${e}-alert--show-icon`,!this.title&&this.closable&&`${e}-alert--right-adjust`,this.rtlEnabled&&`${e}-alert--rtl`],style:this.cssVars,role:`alert`};return this.visible?d(`div`,Object.assign({},i(this.$attrs,n)),this.closable&&d(j,{clsPrefix:e,class:`${e}-alert__close`,onClick:this.handleCloseClick}),this.bordered&&d(`div`,{class:`${e}-alert__border`}),this.showIcon&&d(`div`,{class:`${e}-alert__icon`,"aria-hidden":`true`},N(t.icon,()=>[d(A,{clsPrefix:e},{default:()=>{switch(this.type){case`success`:return d(Y,null);case`info`:return d(X,null);case`warning`:return d(Z,null);case`error`:return d(J,null);default:return null}}})])),d(`div`,{class:[`${e}-alert-body`,this.mergedBordered&&`${e}-alert-body--bordered`]},O(t.header,t=>{let n=t||this.title;return n?d(`div`,{class:`${e}-alert-body__title`},n):null}),t.default&&d(`div`,{class:`${e}-alert-body__content`},t))):null}})}}),re=y([x(`table`,`
 font-size: var(--n-font-size);
 font-variant-numeric: tabular-nums;
 line-height: var(--n-line-height);
 width: 100%;
 border-radius: var(--n-border-radius) var(--n-border-radius) 0 0;
 text-align: left;
 border-collapse: separate;
 border-spacing: 0;
 overflow: hidden;
 background-color: var(--n-td-color);
 border-color: var(--n-merged-border-color);
 transition:
 background-color .3s var(--n-bezier),
 border-color .3s var(--n-bezier),
 color .3s var(--n-bezier);
 --n-merged-border-color: var(--n-border-color);
 `,[y(`th`,`
 white-space: nowrap;
 transition:
 background-color .3s var(--n-bezier),
 border-color .3s var(--n-bezier),
 color .3s var(--n-bezier);
 text-align: inherit;
 padding: var(--n-th-padding);
 vertical-align: inherit;
 text-transform: none;
 border: 0px solid var(--n-merged-border-color);
 font-weight: var(--n-th-font-weight);
 color: var(--n-th-text-color);
 background-color: var(--n-th-color);
 border-bottom: 1px solid var(--n-merged-border-color);
 border-right: 1px solid var(--n-merged-border-color);
 `,[y(`&:last-child`,`
 border-right: 0px solid var(--n-merged-border-color);
 `)]),y(`td`,`
 transition:
 background-color .3s var(--n-bezier),
 border-color .3s var(--n-bezier),
 color .3s var(--n-bezier);
 padding: var(--n-td-padding);
 color: var(--n-td-text-color);
 background-color: var(--n-td-color);
 border: 0px solid var(--n-merged-border-color);
 border-right: 1px solid var(--n-merged-border-color);
 border-bottom: 1px solid var(--n-merged-border-color);
 `,[y(`&:last-child`,`
 border-right: 0px solid var(--n-merged-border-color);
 `)]),T(`bordered`,`
 border: 1px solid var(--n-merged-border-color);
 border-radius: var(--n-border-radius);
 `,[y(`tr`,[y(`&:last-child`,[y(`td`,`
 border-bottom: 0 solid var(--n-merged-border-color);
 `)])])]),T(`single-line`,[y(`th`,`
 border-right: 0px solid var(--n-merged-border-color);
 `),y(`td`,`
 border-right: 0px solid var(--n-merged-border-color);
 `)]),T(`single-column`,[y(`tr`,[y(`&:not(:last-child)`,[y(`td`,`
 border-bottom: 0px solid var(--n-merged-border-color);
 `)])])]),T(`striped`,[y(`tr:nth-of-type(even)`,[y(`td`,`background-color: var(--n-td-color-striped)`)])]),w(`bottom-bordered`,[y(`tr`,[y(`&:last-child`,[y(`td`,`
 border-bottom: 0px solid var(--n-merged-border-color);
 `)])])])]),C(x(`table`,`
 background-color: var(--n-td-color-modal);
 --n-merged-border-color: var(--n-border-color-modal);
 `,[y(`th`,`
 background-color: var(--n-th-color-modal);
 `),y(`td`,`
 background-color: var(--n-td-color-modal);
 `)])),_(x(`table`,`
 background-color: var(--n-td-color-popover);
 --n-merged-border-color: var(--n-border-color-popover);
 `,[y(`th`,`
 background-color: var(--n-th-color-popover);
 `),y(`td`,`
 background-color: var(--n-td-color-popover);
 `)]))]),ie=n({name:`Table`,props:Object.assign(Object.assign({},P.props),{bordered:{type:Boolean,default:!0},bottomBordered:{type:Boolean,default:!0},singleLine:{type:Boolean,default:!0},striped:Boolean,singleColumn:Boolean,size:String}),setup(e){let{mergedClsPrefixRef:t,inlineThemeDisabled:n,mergedRtlRef:r,mergedComponentPropsRef:i}=S(e),a=u(()=>e.size||i?.value?.Table?.size||`medium`),o=P(`Table`,`-table`,re,K,e,t),s=I(`Table`,r,t),c=u(()=>{let e=a.value,{self:{borderColor:t,tdColor:n,tdColorModal:r,tdColorPopover:i,thColor:s,thColorModal:c,thColorPopover:l,thTextColor:u,tdTextColor:d,borderRadius:f,thFontWeight:p,lineHeight:m,borderColorModal:h,borderColorPopover:g,tdColorStriped:_,tdColorStripedModal:v,tdColorStripedPopover:y,[D(`fontSize`,e)]:b,[D(`tdPadding`,e)]:x,[D(`thPadding`,e)]:S},common:{cubicBezierEaseInOut:C}}=o.value;return{"--n-bezier":C,"--n-td-color":n,"--n-td-color-modal":r,"--n-td-color-popover":i,"--n-td-text-color":d,"--n-border-color":t,"--n-border-color-modal":h,"--n-border-color-popover":g,"--n-border-radius":f,"--n-font-size":b,"--n-th-color":s,"--n-th-color-modal":c,"--n-th-color-popover":l,"--n-th-font-weight":p,"--n-th-text-color":u,"--n-line-height":m,"--n-td-padding":x,"--n-th-padding":S,"--n-td-color-striped":_,"--n-td-color-striped-modal":v,"--n-td-color-striped-popover":y}}),l=n?E(`table`,u(()=>a.value[0]),c,e):void 0;return{rtlEnabled:s,mergedClsPrefix:t,cssVars:n?void 0:c,themeClass:l?.themeClass,onRender:l?.onRender}},render(){var e;let{mergedClsPrefix:t}=this;return(e=this.onRender)==null||e.call(this),d(`table`,{class:[`${t}-table`,this.themeClass,{[`${t}-table--rtl`]:this.rtlEnabled,[`${t}-table--bottom-bordered`]:this.bottomBordered,[`${t}-table--bordered`]:this.bordered,[`${t}-table--single-line`]:this.singleLine,[`${t}-table--single-column`]:this.singleColumn,[`${t}-table--striped`]:this.striped}],style:this.cssVars},this.$slots)}});async function ae(e){let{data:t}=await Q.post(`/api/admin-next/legacy-sync/preview`,e);return t}async function oe(e,t){let{data:n}=await Q.post(`/api/admin-next/legacy-sync/import`,{...e,jobIds:t});return n}var se={class:`page-stack`},ce={class:`table-actions`},le={class:`text-12px text-gray-500`},ue={class:`max-w-240px truncate`},de=n({name:`legacy-sync`,__name:`index`,setup(n){let i=G(),d=f(!1),_=f(!1),v=f(null),y=f([]),b=f({sourceUrl:``,username:``,password:``}),x=u(()=>!!v.value?.jobs.length&&y.value.length===v.value?.jobs.length);function S(e){y.value=e&&v.value?.jobs.map(e=>e.id)||[]}function C(e,t){y.value=t?[...y.value,e.id]:y.value.filter(t=>t!==e.id)}async function w(){if(!b.value.sourceUrl.trim()||!b.value.username.trim()||!b.value.password){i.error(`请填写旧平台地址、账号和密码`);return}d.value=!0;try{let e=await ae({...b.value,sourceUrl:b.value.sourceUrl.trim(),username:b.value.username.trim()});if(e.code!==200)throw Error(e.msg||`旧平台预览失败`);v.value=e.data,y.value=e.data.jobs.map(e=>e.id),i.success(`已读取 ${e.data.groups.length} 个执行器、${e.data.jobs.length} 个任务`)}catch(e){i.error(e.message||`旧平台预览失败`)}finally{d.value=!1}}async function T(){if(!y.value.length){i.error(`请至少选择一个任务`);return}_.value=!0;try{let e=await oe(b.value,y.value);if(e.code!==200)throw Error(e.msg||`任务导入失败`);i.success(`导入完成：新增 ${e.data.jobsCreated} 个，跳过 ${e.data.jobsSkipped} 个`),e.data.warning&&i.warning(e.data.warning,{duration:8e3}),await w()}catch(e){i.error(e.message||`任务导入失败`)}finally{_.value=!1}}return(n,i)=>(r(),c(`div`,se,[h(m(M),{bordered:!1,title:`旧平台任务同步`},{default:e(()=>[h(m(V),{"label-placement":`left`,"label-width":`110`},{default:e(()=>[h(m(H),{label:`旧平台地址`},{default:e(()=>[h(m(R),{value:b.value.sourceUrl,"onUpdate:value":i[0]||=e=>b.value.sourceUrl=e,placeholder:`例如 http://127.0.0.1:8080/xxl-job-admin/`},null,8,[`value`])]),_:1}),h(m(H),{label:`旧平台账号`},{default:e(()=>[h(m(R),{value:b.value.username,"onUpdate:value":i[1]||=e=>b.value.username=e,autocomplete:`username`,placeholder:`旧 admin 用户名`},null,8,[`value`])]),_:1}),h(m(H),{label:`旧平台密码`},{default:e(()=>[h(m(R),{value:b.value.password,"onUpdate:value":i[2]||=e=>b.value.password=e,type:`password`,"show-password-on":`click`,autocomplete:`current-password`},null,8,[`value`])]),_:1}),s(`div`,ce,[h(m(B),{type:`primary`,loading:d.value,onClick:w},{default:e(()=>[...i[3]||=[t(`连接并预览`,-1)]]),_:1},8,[`loading`]),h(m(B),{disabled:!v.value||!y.value.length,loading:_.value,onClick:T},{default:e(()=>[...i[4]||=[t(`导入选中任务`,-1)]]),_:1},8,[`disabled`,`loading`])])]),_:1}),h(m(ne),{type:`warning`,class:`mt-16px`},{default:e(()=>[...i[5]||=[t(` 账号密码只用于本次请求，不会保存。导入采用只新增策略，已有 AppName + JobHandler 会跳过；所有新任务默认停用，避免新旧平台重复执行。 `,-1)]]),_:1})]),_:1}),v.value?(r(),p(m(M),{key:0,bordered:!1,title:`任务预览（已选 ${y.value.length} / ${v.value.jobs.length}）`},{default:e(()=>[h(m(ie),{bordered:!1,"single-line":!1},{default:e(()=>[s(`thead`,null,[s(`tr`,null,[s(`th`,null,[h(m(U),{checked:x.value,"onUpdate:checked":S},null,8,[`checked`])]),i[6]||=s(`th`,null,`执行器`,-1),i[7]||=s(`th`,null,`任务描述`,-1),i[8]||=s(`th`,null,`JobHandler`,-1),i[9]||=s(`th`,null,`调度配置`,-1),i[10]||=s(`th`,null,`参数`,-1)])]),s(`tbody`,null,[(r(!0),c(l,null,o(v.value.jobs,e=>(r(),c(`tr`,{key:e.id},[s(`td`,null,[h(m(U),{checked:y.value.includes(e.id),"onUpdate:checked":t=>C(e,t)},null,8,[`checked`,`onUpdate:checked`])]),s(`td`,null,[t(a(e.appname),1),i[11]||=s(`br`,null,null,-1),s(`span`,le,a(e.groupTitle),1)]),s(`td`,null,a(e.jobDesc),1),s(`td`,null,a(e.executorHandler),1),s(`td`,null,a(e.scheduleType)+` / `+a(e.scheduleConf||`-`),1),s(`td`,ue,a(e.executorParam||`-`),1)]))),128))])]),_:1})]),_:1},8,[`title`])):g(``,!0)]))}});export{de as default};