import{E as e,U as t,g as n,ht as r,j as i,k as a,ut as o}from"./vue.runtime.esm-bundler-CoGLJnd5.js";import{$t as s,Gt as c,Jt as l,Kt as u,Q as d,Qt as f,Yt as p,Z as m,Zt as h,_t as g,at as _,ct as v,f as y,ht as b,v as x,vt as S,x as C}from"./axios-BwS9P_IR.js";import{g as w,h as T}from"./Input-ZK5LI6Wg.js";import{f as E,h as D,p as O,y as k}from"./Button-BoJU_Zkc.js";var A={sizeSmall:`14px`,sizeMedium:`16px`,sizeLarge:`18px`,labelPadding:`0 8px`,labelFontWeight:`400`};function j(e){let{baseColor:t,inputColorDisabled:n,cardColor:r,modalColor:i,popoverColor:a,textColorDisabled:o,borderColor:s,primaryColor:c,textColor2:l,fontSizeSmall:u,fontSizeMedium:d,fontSizeLarge:f,borderRadiusSmall:p,lineHeight:m}=e;return Object.assign(Object.assign({},A),{labelLineHeight:m,fontSizeSmall:u,fontSizeMedium:d,fontSizeLarge:f,borderRadius:p,color:t,colorChecked:c,colorDisabled:n,colorDisabledChecked:n,colorTableHeader:r,colorTableHeaderModal:i,colorTableHeaderPopover:a,checkMarkColor:t,checkMarkColorDisabled:o,checkMarkColorDisabledChecked:o,border:`1px solid ${s}`,borderDisabled:`1px solid ${s}`,borderDisabledChecked:`1px solid ${s}`,borderChecked:`1px solid ${c}`,borderFocus:`1px solid ${c}`,boxShadowFocus:`0 0 0 2px ${S(c,{alpha:.3})}`,textColor:l,textColorDisabled:o})}var M={name:`Checkbox`,common:y,self:j},N=b(`n-checkbox-group`),P=e({name:`CheckboxGroup`,props:{min:Number,max:Number,size:String,value:Array,defaultValue:{type:Array,default:null},disabled:{type:Boolean,default:void 0},"onUpdate:value":[Function,Array],onUpdateValue:[Function,Array],onChange:[Function,Array]},setup(e){let{mergedClsPrefixRef:i}=d(e),a=D(e),{mergedSizeRef:s,mergedDisabledRef:c}=a,l=o(e.defaultValue),u=T(n(()=>e.value),l),f=n(()=>u.value?.length||0),p=n(()=>Array.isArray(u.value)?new Set(u.value):new Set);function m(t,n){let{nTriggerFormInput:r,nTriggerFormChange:i}=a,{onChange:o,"onUpdate:value":s,onUpdateValue:c}=e;if(Array.isArray(u.value)){let e=Array.from(u.value),a=e.findIndex(e=>e===n);t?~a||(e.push(n),c&&v(c,e,{actionType:`check`,value:n}),s&&v(s,e,{actionType:`check`,value:n}),r(),i(),l.value=e,o&&v(o,e)):~a&&(e.splice(a,1),c&&v(c,e,{actionType:`uncheck`,value:n}),s&&v(s,e,{actionType:`uncheck`,value:n}),o&&v(o,e),l.value=e,r(),i())}else t?(c&&v(c,[n],{actionType:`check`,value:n}),s&&v(s,[n],{actionType:`check`,value:n}),o&&v(o,[n]),l.value=[n],r(),i()):(c&&v(c,[],{actionType:`uncheck`,value:n}),s&&v(s,[],{actionType:`uncheck`,value:n}),o&&v(o,[]),l.value=[],r(),i())}return t(N,{checkedCountRef:f,maxRef:r(e,`max`),minRef:r(e,`min`),valueSetRef:p,disabledRef:c,mergedSizeRef:s,toggleCheckbox:m}),{mergedClsPrefix:i}},render(){return a(`div`,{class:`${this.mergedClsPrefix}-checkbox-group`,role:`group`},this.$slots)}}),F=()=>a(`svg`,{viewBox:`0 0 64 64`,class:`check-icon`},a(`path`,{d:`M50.42,16.76L22.34,39.45l-8.1-11.46c-1.12-1.58-3.3-1.96-4.88-0.84c-1.58,1.12-1.95,3.3-0.84,4.88l10.26,14.51  c0.56,0.79,1.42,1.31,2.38,1.45c0.16,0.02,0.32,0.03,0.48,0.03c0.8,0,1.57-0.27,2.2-0.78l30.99-25.03c1.5-1.21,1.74-3.42,0.52-4.92  C54.13,15.78,51.93,15.55,50.42,16.76z`})),I=()=>a(`svg`,{viewBox:`0 0 100 100`,class:`line-icon`},a(`path`,{d:`M80.2,55.5H21.4c-2.8,0-5.1-2.5-5.1-5.5l0,0c0-3,2.3-5.5,5.1-5.5h58.7c2.8,0,5.1,2.5,5.1,5.5l0,0C85.2,53.1,82.9,55.5,80.2,55.5z`})),L=c([u(`checkbox`,`
 font-size: var(--n-font-size);
 outline: none;
 cursor: pointer;
 display: inline-flex;
 flex-wrap: nowrap;
 align-items: flex-start;
 word-break: break-word;
 line-height: var(--n-size);
 --n-merged-color-table: var(--n-color-table);
 `,[p(`show-label`,`line-height: var(--n-label-line-height);`),c(`&:hover`,[u(`checkbox-box`,[l(`border`,`border: var(--n-border-checked);`)])]),c(`&:focus:not(:active)`,[u(`checkbox-box`,[l(`border`,`
 border: var(--n-border-focus);
 box-shadow: var(--n-box-shadow-focus);
 `)])]),p(`inside-table`,[u(`checkbox-box`,`
 background-color: var(--n-merged-color-table);
 `)]),p(`checked`,[u(`checkbox-box`,`
 background-color: var(--n-color-checked);
 `,[u(`checkbox-icon`,[c(`.check-icon`,`
 opacity: 1;
 transform: scale(1);
 `)])])]),p(`indeterminate`,[u(`checkbox-box`,[u(`checkbox-icon`,[c(`.check-icon`,`
 opacity: 0;
 transform: scale(.5);
 `),c(`.line-icon`,`
 opacity: 1;
 transform: scale(1);
 `)])])]),p(`checked, indeterminate`,[c(`&:focus:not(:active)`,[u(`checkbox-box`,[l(`border`,`
 border: var(--n-border-checked);
 box-shadow: var(--n-box-shadow-focus);
 `)])]),u(`checkbox-box`,`
 background-color: var(--n-color-checked);
 border-left: 0;
 border-top: 0;
 `,[l(`border`,{border:`var(--n-border-checked)`})])]),p(`disabled`,{cursor:`not-allowed`},[p(`checked`,[u(`checkbox-box`,`
 background-color: var(--n-color-disabled-checked);
 `,[l(`border`,{border:`var(--n-border-disabled-checked)`}),u(`checkbox-icon`,[c(`.check-icon, .line-icon`,{fill:`var(--n-check-mark-color-disabled-checked)`})])])]),u(`checkbox-box`,`
 background-color: var(--n-color-disabled);
 `,[l(`border`,`
 border: var(--n-border-disabled);
 `),u(`checkbox-icon`,[c(`.check-icon, .line-icon`,`
 fill: var(--n-check-mark-color-disabled);
 `)])]),l(`label`,`
 color: var(--n-text-color-disabled);
 `)]),u(`checkbox-box-wrapper`,`
 position: relative;
 width: var(--n-size);
 flex-shrink: 0;
 flex-grow: 0;
 user-select: none;
 -webkit-user-select: none;
 `),u(`checkbox-box`,`
 position: absolute;
 left: 0;
 top: 50%;
 transform: translateY(-50%);
 height: var(--n-size);
 width: var(--n-size);
 display: inline-block;
 box-sizing: border-box;
 border-radius: var(--n-border-radius);
 background-color: var(--n-color);
 transition: background-color 0.3s var(--n-bezier);
 `,[l(`border`,`
 transition:
 border-color .3s var(--n-bezier),
 box-shadow .3s var(--n-bezier);
 border-radius: inherit;
 position: absolute;
 left: 0;
 right: 0;
 top: 0;
 bottom: 0;
 border: var(--n-border);
 `),u(`checkbox-icon`,`
 display: flex;
 align-items: center;
 justify-content: center;
 position: absolute;
 left: 1px;
 right: 1px;
 top: 1px;
 bottom: 1px;
 `,[c(`.check-icon, .line-icon`,`
 width: 100%;
 fill: var(--n-check-mark-color);
 opacity: 0;
 transform: scale(0.5);
 transform-origin: center;
 transition:
 fill 0.3s var(--n-bezier),
 transform 0.3s var(--n-bezier),
 opacity 0.3s var(--n-bezier),
 border-color 0.3s var(--n-bezier);
 `),E({left:`1px`,top:`1px`})])]),l(`label`,`
 color: var(--n-text-color);
 transition: color .3s var(--n-bezier);
 user-select: none;
 -webkit-user-select: none;
 padding: var(--n-label-padding);
 font-weight: var(--n-label-font-weight);
 `,[c(`&:empty`,{display:`none`})])]),f(u(`checkbox`,`
 --n-merged-color-table: var(--n-color-table-modal);
 `)),s(u(`checkbox`,`
 --n-merged-color-table: var(--n-color-table-popover);
 `))]),R=e({name:`Checkbox`,props:Object.assign(Object.assign({},x.props),{size:String,checked:{type:[Boolean,String,Number],default:void 0},defaultChecked:{type:[Boolean,String,Number],default:!1},value:[String,Number],disabled:{type:Boolean,default:void 0},indeterminate:Boolean,label:String,focusable:{type:Boolean,default:!0},checkedValue:{type:[Boolean,String,Number],default:!0},uncheckedValue:{type:[Boolean,String,Number],default:!1},"onUpdate:checked":[Function,Array],onUpdateChecked:[Function,Array],privateInsideTable:Boolean,onChange:[Function,Array]}),setup(e){let t=i(N,null),a=o(null),{mergedClsPrefixRef:s,inlineThemeDisabled:c,mergedRtlRef:l,mergedComponentPropsRef:u}=d(e),f=o(e.defaultChecked),p=T(r(e,`checked`),f),g=k(()=>{if(t){let n=t.valueSetRef.value;return n&&e.value!==void 0?n.has(e.value):!1}else return p.value===e.checkedValue}),_=D(e,{mergedSize(n){let{size:r}=e;if(r!==void 0)return r;if(t){let{value:e}=t.mergedSizeRef;if(e!==void 0)return e}if(n){let{mergedSize:e}=n;if(e!==void 0)return e.value}return u?.value?.Checkbox?.size||`medium`},mergedDisabled(n){let{disabled:r}=e;if(r!==void 0)return r;if(t){if(t.disabledRef.value)return!0;let{maxRef:{value:e},checkedCountRef:n}=t;if(e!==void 0&&n.value>=e&&!g.value)return!0;let{minRef:{value:r}}=t;if(r!==void 0&&n.value<=r&&g.value)return!0}return n?n.disabled.value:!1}}),{mergedDisabledRef:y,mergedSizeRef:b}=_,S=x(`Checkbox`,`-checkbox`,L,M,e,s);function E(n){if(t&&e.value!==void 0)t.toggleCheckbox(!g.value,e.value);else{let{onChange:t,"onUpdate:checked":r,onUpdateChecked:i}=e,{nTriggerFormInput:a,nTriggerFormChange:o}=_,s=g.value?e.uncheckedValue:e.checkedValue;r&&v(r,s,n),i&&v(i,s,n),t&&v(t,s,n),a(),o(),f.value=s}}function O(e){y.value||E(e)}function A(e){if(!y.value)switch(e.key){case` `:case`Enter`:E(e)}}function j(e){switch(e.key){case` `:e.preventDefault()}}let P={focus:()=>{var e;(e=a.value)==null||e.focus()},blur:()=>{var e;(e=a.value)==null||e.blur()}},F=C(`Checkbox`,l,s),I=n(()=>{let{value:e}=b,{common:{cubicBezierEaseInOut:t},self:{borderRadius:n,color:r,colorChecked:i,colorDisabled:a,colorTableHeader:o,colorTableHeaderModal:s,colorTableHeaderPopover:c,checkMarkColor:l,checkMarkColorDisabled:u,border:d,borderFocus:f,borderDisabled:p,borderChecked:m,boxShadowFocus:g,textColor:_,textColorDisabled:v,checkMarkColorDisabledChecked:y,colorDisabledChecked:x,borderDisabledChecked:C,labelPadding:w,labelLineHeight:T,labelFontWeight:E,[h(`fontSize`,e)]:D,[h(`size`,e)]:O}}=S.value;return{"--n-label-line-height":T,"--n-label-font-weight":E,"--n-size":O,"--n-bezier":t,"--n-border-radius":n,"--n-border":d,"--n-border-checked":m,"--n-border-focus":f,"--n-border-disabled":p,"--n-border-disabled-checked":C,"--n-box-shadow-focus":g,"--n-color":r,"--n-color-checked":i,"--n-color-table":o,"--n-color-table-modal":s,"--n-color-table-popover":c,"--n-color-disabled":a,"--n-color-disabled-checked":x,"--n-text-color":_,"--n-text-color-disabled":v,"--n-check-mark-color":l,"--n-check-mark-color-disabled":u,"--n-check-mark-color-disabled-checked":y,"--n-font-size":D,"--n-label-padding":w}}),R=c?m(`checkbox`,n(()=>b.value[0]),I,e):void 0;return Object.assign(_,P,{rtlEnabled:F,selfRef:a,mergedClsPrefix:s,mergedDisabled:y,renderedChecked:g,mergedTheme:S,labelId:w(),handleClick:O,handleKeyUp:A,handleKeyDown:j,cssVars:c?void 0:I,themeClass:R?.themeClass,onRender:R?.onRender})},render(){var e;let{$slots:t,renderedChecked:n,mergedDisabled:r,indeterminate:i,privateInsideTable:o,cssVars:s,labelId:c,label:l,mergedClsPrefix:u,focusable:d,handleKeyUp:f,handleKeyDown:p,handleClick:m}=this;(e=this.onRender)==null||e.call(this);let h=_(t.default,e=>l||e?a(`span`,{class:`${u}-checkbox__label`,id:c},l||e):null);return a(`div`,{ref:`selfRef`,class:[`${u}-checkbox`,this.themeClass,this.rtlEnabled&&`${u}-checkbox--rtl`,n&&`${u}-checkbox--checked`,r&&`${u}-checkbox--disabled`,i&&`${u}-checkbox--indeterminate`,o&&`${u}-checkbox--inside-table`,h&&`${u}-checkbox--show-label`],tabindex:r||!d?void 0:0,role:`checkbox`,"aria-checked":i?`mixed`:n,"aria-labelledby":c,style:s,onKeyup:f,onKeydown:p,onClick:m,onMousedown:()=>{g(`selectstart`,window,e=>{e.preventDefault()},{once:!0})}},a(`div`,{class:`${u}-checkbox-box-wrapper`},`\xA0`,a(`div`,{class:`${u}-checkbox-box`},a(O,null,{default:()=>this.indeterminate?a(`div`,{key:`indeterminate`,class:`${u}-checkbox-icon`},I()):a(`div`,{key:`check`,class:`${u}-checkbox-icon`},F())}),a(`div`,{class:`${u}-checkbox-box__border`}))),h)}});export{j as i,P as n,M as r,R as t};