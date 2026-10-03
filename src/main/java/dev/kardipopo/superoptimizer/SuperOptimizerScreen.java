package dev.kardipopo.superoptimizer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

/** Sodium-style top tabs + Iris-style profile selector. */
public final class SuperOptimizerScreen extends Screen {
    private enum Category {
        GENERAL("superoptimizer.category.general"),
        CULLING("superoptimizer.category.culling"),
        CPU("superoptimizer.category.cpu"),
        COMPATIBILITY("superoptimizer.category.compatibility"),
        SHADERS("superoptimizer.shaders.title"),
        DIAGNOSTICS("superoptimizer.category.diagnostics");
        final String key;
        Category(String key){this.key=key;}
    }
    private enum Kind { TOGGLE, VALUE, ACTION, STATUS }
    private record Row(Button button,String key,String desc,Supplier<Component> value,int y,Kind kind){}
    private record Tab(Category category,Button button,int x,int w){}
    private record Profile(SuperOptimizerClient.Preset preset,Button button,int y,int h){}

    private final Screen parent;
    private final SuperOptimizerConfig config;
    private final Category category;
    private final List<Row> rows=new ArrayList<>();
    private final List<Tab> tabs=new ArrayList<>();
    private final List<Profile> profiles=new ArrayList<>();
    private final List<Button> fixed=new ArrayList<>();
    private final List<Section> sections=new ArrayList<>();
    private record Section(String title,String subtitle,int y){}

    private double scroll,maxScroll;
    private int mainX,mainW,sideX,sideW,top,bottom,pageBottom;
    private SuperOptimizerClient.Preset activePreset;

    public SuperOptimizerScreen(Screen parent,SuperOptimizerConfig config){this(parent,config,Category.GENERAL);}
    public SuperOptimizerScreen(Screen parent,SuperOptimizerConfig config,Category category){
        super(Component.translatable("superoptimizer.gui.title"));
        this.parent=parent;this.config=config;this.category=category;this.activePreset=detectPreset();
    }

    @Override protected void init(){
        rows.clear();tabs.clear();profiles.clear();fixed.clear();sections.clear();scroll=0;maxScroll=0;
        int margin=20,gap=14;
        sideW=Math.min(286,Math.max(0,this.width/3));
        mainW=this.width-margin*2-gap-sideW;
        if(mainW<430){sideW=Math.max(0,this.width-margin*2-gap-430);mainW=this.width-margin*2-gap-sideW;}
        if(sideW<210){sideW=0;mainW=this.width-margin*2;}
        mainX=margin;sideX=mainX+mainW+gap;top=96;bottom=this.height-56;
        buildTabs(margin,46);buildPage();buildFixed();if(sideW>0)buildProfiles();
        maxScroll=Math.max(0,pageBottom-(bottom-6));applyScroll();
    }

    private void buildTabs(int x,int y){
        int n=Category.values().length,gap=6,w=Math.max(80,(this.width-40-(n-1)*gap)/n);
        for(Category c:Category.values()){
            int tx=x+tabs.size()*(w+gap);
            Button b=hit(Component.translatable(c.key),q->minecraft.setScreenAndShow(new SuperOptimizerScreen(parent,config,c)),tx,y,w,30);
            tabs.add(new Tab(c,b,tx,w));
        }
    }

    private void buildPage(){
        int y=top+44;
        switch(category){
            case GENERAL->y=general(y);
            case CULLING->y=culling(y);
            case CPU->y=cpu(y);
            case COMPATIBILITY->y=compat(y);
            case SHADERS->y=shaders(y);
            case DIAGNOSTICS->y=diagnostics(y);
        }
        pageBottom=y;
    }

    private int general(int y){
        y=section(y,"superoptimizer.category.general","superoptimizer.desc.enabled");
        y=toggle(y,"superoptimizer.option.enabled","superoptimizer.desc.enabled",()->config.enabled,v->{config.enabled=v;manual();SuperOptimizerClient.applyConfig();});
        y=toggle(y,"superoptimizer.option.background","superoptimizer.desc.background",()->config.backgroundTasks,v->{config.backgroundTasks=v;manual();SuperOptimizerClient.applyConfig();});
        y=toggle(y,"superoptimizer.option.pause_motion","superoptimizer.desc.pause_motion",()->config.pauseDuringCameraMotion,v->{config.pauseDuringCameraMotion=v;manual();});
        y=toggle(y,"superoptimizer.option.skip_near","superoptimizer.desc.skip_near",()->config.skipNearEntityCulling,v->{config.skipNearEntityCulling=v;manual();});
        y=toggle(y,"superoptimizer.option.directional","superoptimizer.desc.directional",()->config.directionalEntityCulling,v->{config.directionalEntityCulling=v;manual();});
        return y;
    }
    private int culling(int y){
        y=section(y,"superoptimizer.category.culling","superoptimizer.desc.entity");
        y=toggle(y,"superoptimizer.option.entity","superoptimizer.desc.entity",()->config.entityCulling,v->{config.entityCulling=v;manual();});
        y=toggle(y,"superoptimizer.option.block_entity","superoptimizer.desc.block_entity",()->config.blockEntityCulling,v->{config.blockEntityCulling=v;manual();});
        y=toggle(y,"superoptimizer.option.skip_near","superoptimizer.desc.skip_near",()->config.skipNearEntityCulling,v->{config.skipNearEntityCulling=v;manual();});
        y=cycle(y,"superoptimizer.option.near_distance","superoptimizer.desc.near_distance",()->config.nearEntityDistance,0,64,4,v->{config.nearEntityDistance=v;manual();});
        y=toggle(y,"superoptimizer.option.directional","superoptimizer.desc.directional",()->config.directionalEntityCulling,v->{config.directionalEntityCulling=v;manual();});
        return y;
    }
    private int cpu(int y){
        y=section(y,"superoptimizer.category.cpu","superoptimizer.desc.workers");
        y=toggle(y,"superoptimizer.option.background","superoptimizer.desc.background",()->config.backgroundTasks,v->{config.backgroundTasks=v;manual();SuperOptimizerClient.applyConfig();});
        y=cycle(y,"superoptimizer.option.workers","superoptimizer.desc.workers",()->config.workerThreads,1,Math.max(1,Math.min(16,Runtime.getRuntime().availableProcessors())),1,v->{config.workerThreads=v;manual();SuperOptimizerClient.applyConfig();});
        y=cycle(y,"superoptimizer.option.reserved","superoptimizer.desc.reserved",()->config.reservedCores,0,Math.min(8,Math.max(0,Runtime.getRuntime().availableProcessors()-1)),1,v->{config.reservedCores=v;manual();SuperOptimizerClient.applyConfig();});
        y=toggle(y,"superoptimizer.option.shader_scan_async","superoptimizer.desc.shader_scan_async",()->config.shaderScanAsync,v->{config.shaderScanAsync=v;manual();SuperOptimizerClient.applyConfig();});
        return y;
    }
    private int compat(int y){
        y=section(y,"superoptimizer.category.compatibility","superoptimizer.desc.iris_lock");
        y=toggle(y,"superoptimizer.option.iris_lock","superoptimizer.desc.iris_lock",()->config.disableCullingWithIris,v->{config.disableCullingWithIris=v;manual();});
        y=toggle(y,"superoptimizer.option.entity_culling_lock","superoptimizer.desc.entity_culling_lock",()->config.disableCullingWithEntityCullingMod,v->{config.disableCullingWithEntityCullingMod=v;manual();});
        y=status(y,"superoptimizer.compat.iris",()->Component.literal(net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("iris")?"обнаружен":"не установлен"));
        y=status(y,"superoptimizer.compat.entity_culling",()->Component.literal(net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("entityculling")?"обнаружен":"не установлен"));
        return y;
    }
    private int shaders(int y){
        y=section(y,"superoptimizer.shaders.title","superoptimizer.desc.shader_iris");
        boolean iris=net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("iris");
        y=toggle(y,"superoptimizer.option.shader_scan_async","superoptimizer.desc.shader_scan_async",()->config.shaderScanAsync,v->{config.shaderScanAsync=v;manual();SuperOptimizerClient.applyConfig();});
        y=status(y,"superoptimizer.shaders.iris_detected",()->Component.literal(iris?"Iris обнаружен":"Iris не найден"));
        int by=y+4;
        Button b=hit(Component.translatable("superoptimizer.button.shaders"),q->minecraft.setScreenAndShow(new ShaderPackScreen(this,config)),mainX+14,by,mainW-28,44);
        b.setTooltip(Tooltip.create(Component.translatable("superoptimizer.desc.shader_iris")));b.setTooltipDelay(Duration.ofMillis(250));
        rows.add(new Row(b,"superoptimizer.button.shaders","superoptimizer.desc.shader_iris",()->Component.literal(iris?"Открыть":"Недоступно"),by,Kind.ACTION));
        return by+58;
    }
    private int diagnostics(int y){
        y=section(y,"superoptimizer.category.diagnostics","superoptimizer.desc.diagnostics");
        y=toggle(y,"superoptimizer.option.diagnostics","superoptimizer.desc.diagnostics",()->config.diagnostics,v->{config.diagnostics=v;manual();});
        y=toggle(y,"superoptimizer.option.file_logging","superoptimizer.desc.file_logging",()->config.fileLogging,v->{config.fileLogging=v;manual();});
        y=status(y,"superoptimizer.diag.entity",()->Component.literal(CullingContext.entityCulled()+" / "+CullingContext.entityChecks()));
        y=status(y,"superoptimizer.diag.block_entity",()->Component.literal(CullingContext.blockEntityCulled()+" / "+CullingContext.blockEntityChecks()));
        int ay=y+2,half=(mainW-46)/2;
        Button clear=hit(Component.translatable("superoptimizer.action.clear_logs"),q->SuperOptimizerLog.clear(),mainX+14,ay,half,40);
        Button reset=hit(Component.translatable("superoptimizer.action.reset_stats"),q->CullingContext.resetStats(),mainX+32+half,ay,half,40);
        rows.add(new Row(clear,"superoptimizer.action.clear_logs","superoptimizer.desc.clear_logs",()->Component.literal("Выполнить"),ay,Kind.ACTION));
        rows.add(new Row(reset,"superoptimizer.action.reset_stats","superoptimizer.desc.reset_stats",()->Component.literal("Выполнить"),ay,Kind.ACTION));
        return ay+56;
    }

    private int section(int y,String title,String subtitle){sections.add(new Section(title,subtitle,y));return y+42;}
    private int toggle(int y,String key,String desc,Supplier<Boolean> get,Consumer<Boolean> set){
        Button b=hit(Component.translatable(key),q->set.accept(!get.get()),mainX+14,y,mainW-28,54);
        b.setTooltip(Tooltip.create(Component.translatable(desc)));b.setTooltipDelay(Duration.ofMillis(250));
        rows.add(new Row(b,key,desc,()->Component.literal(get.get()?"ВКЛ":"ВЫКЛ"),y,Kind.TOGGLE));return y+60;
    }
    private int cycle(int y,String key,String desc,Supplier<Integer> get,int min,int max,int step,IntConsumer set){
        Button b=hit(Component.translatable(key),q->{int v=get.get()+step;if(v>max)v=min;set.accept(v);},mainX+14,y,mainW-28,54);
        b.setTooltip(Tooltip.create(Component.translatable(desc)));b.setTooltipDelay(Duration.ofMillis(250));
        rows.add(new Row(b,key,desc,()->Component.literal(Integer.toString(get.get())),y,Kind.VALUE));return y+60;
    }
    private int status(int y,String key,Supplier<Component> value){
        Button b=hit(Component.translatable(key),q->{},mainX+14,y,mainW-28,44);b.active=false;
        rows.add(new Row(b,key,key,value,y,Kind.STATUS));return y+50;
    }
    private Button hit(Component label,Consumer<Button> action,int x,int y,int w,int h){Button b=Button.builder(label,action).bounds(x,y,Math.max(1,w),h).build();b.setAlpha(0f);addRenderableWidget(b);return b;}

    private void buildProfiles(){
        int y=166;
        for(SuperOptimizerClient.Preset p:new SuperOptimizerClient.Preset[]{SuperOptimizerClient.Preset.LIGHT,SuperOptimizerClient.Preset.BALANCED,SuperOptimizerClient.Preset.ADVANCED,SuperOptimizerClient.Preset.MICROWAVE}){
            int ry=y;
            Button b=hit(profileName(p),q->{activePreset=p;SuperOptimizerClient.applyPreset(p);minecraft.setScreenAndShow(new SuperOptimizerScreen(parent,config,category));},sideX+14,ry,sideW-28,62);
            profiles.add(new Profile(p,b,ry,62));y+=68;
        }
        Button save=hit(Component.literal("Сохранить как пользовательский"),q->{config.save(minecraft.gameDirectory.toPath().resolve("config"));activePreset=null;},sideX+14,y+38,sideW-28,32);
        Button reset=hit(Component.literal("Сбросить в сбалансированный"),q->{SuperOptimizerClient.applyPreset(SuperOptimizerClient.Preset.BALANCED);activePreset=SuperOptimizerClient.Preset.BALANCED;minecraft.setScreenAndShow(new SuperOptimizerScreen(parent,config,category));},sideX+14,y+76,sideW-28,32);
        fixed.add(save);fixed.add(reset);
    }
    private Component profileName(SuperOptimizerClient.Preset p){return switch(p){case MICROWAVE->Component.literal("Микроволновка");case LIGHT->Component.translatable("superoptimizer.preset.light");case BALANCED->Component.translatable("superoptimizer.preset.balanced");case ADVANCED->Component.literal("Максимум FPS");};}
    private Component profileSub(SuperOptimizerClient.Preset p){return switch(p){case MICROWAVE->Component.literal("Максимум производительности");case LIGHT->Component.literal("Минимальное вмешательство");case BALANCED->Component.literal("Производительность + стабильность");case ADVANCED->Component.literal("Агрессивная безопасная оптимизация");};}

    private void buildFixed(){
        Button logs=hit(Component.translatable("superoptimizer.button.logs"),q->minecraft.setScreenAndShow(new SuperOptimizerLogScreen(this)),mainX+14,this.height-42,120,28);
        Button done=hit(Component.translatable("gui.done"),q->close(),Math.max(mainX+mainW-156,mainX+150),this.height-42,140,28);
        fixed.add(logs);fixed.add(done);
    }
    private void manual(){activePreset=null;}
    private SuperOptimizerClient.Preset detectPreset(){
        if(match(SuperOptimizerClient.Preset.MICROWAVE))return SuperOptimizerClient.Preset.MICROWAVE;
        if(match(SuperOptimizerClient.Preset.LIGHT))return SuperOptimizerClient.Preset.LIGHT;
        if(match(SuperOptimizerClient.Preset.BALANCED))return SuperOptimizerClient.Preset.BALANCED;
        if(match(SuperOptimizerClient.Preset.ADVANCED))return SuperOptimizerClient.Preset.ADVANCED;return null;
    }
    private boolean match(SuperOptimizerClient.Preset p){return switch(p){
        case MICROWAVE->config.entityCulling&&config.blockEntityCulling&&config.directionalEntityCulling&&!config.backgroundTasks&&!config.shaderScanAsync&&!config.diagnostics&&config.reservedCores==1&&config.workerThreads==1;
        case LIGHT->config.entityCulling&&!config.blockEntityCulling&&config.skipNearEntityCulling&&config.nearEntityDistance==12&&config.directionalEntityCulling&&config.backgroundTasks&&config.shaderScanAsync&&config.diagnostics&&!config.fileLogging&&config.workerThreads<=2;
        case BALANCED->config.entityCulling&&config.blockEntityCulling&&config.skipNearEntityCulling&&config.nearEntityDistance==12&&config.directionalEntityCulling&&config.backgroundTasks&&config.shaderScanAsync&&config.diagnostics&&config.fileLogging&&config.workerThreads<=3;
        case ADVANCED->config.entityCulling&&config.blockEntityCulling&&!config.skipNearEntityCulling&&config.nearEntityDistance==0&&config.directionalEntityCulling&&config.backgroundTasks&&config.shaderScanAsync&&config.diagnostics&&config.fileLogging&&config.workerThreads>=2;
    };}

    private void applyScroll(){for(Row r:rows)r.button().setY(r.y()-(int)scroll);}
    @Override public boolean mouseScrolled(double x,double y,double hx,double vy){
        if(y>=top-2&&y<=bottom&&maxScroll>0){scroll=Math.max(0,Math.min(maxScroll,scroll-vy*34));applyScroll();return true;}
        return super.mouseScrolled(x,y,hx,vy);
    }

    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){
        super.extractRenderState(g,mx,my,delta);
        g.fill(0,0,width,height,0xE5081220);g.fill(0,0,width,3,0xFF20D7C7);
        g.text(font,title,22,14,0xFFF3F7FF,true);g.text(font,Component.translatable("superoptimizer.gui.subtitle"),22,28,0xFFA4B0C6,false);
        g.text(font,Component.translatable("superoptimizer.gui.backend",Minecraft.getInstance().options.preferredGraphicsBackend().toString()),Math.max(22,width-240),21,0xFF9DAAC1,false);
        drawTabs(g,mx,my);drawMain(g,mx,my);if(sideW>0)drawSide(g,mx,my);drawFooter(g);
        Row hover=hoveredRow(mx,my);if(hover!=null&&sideW>0)drawHover(g,hover);
    }
    private void drawTabs(GuiGraphicsExtractor g,int mx,int my){for(Tab t:tabs){boolean sel=t.category()==category,hov=mx>=t.x()&&mx<t.x()+t.w()&&my>=46&&my<76;g.fill(t.x(),46,t.x()+t.w(),76,sel?0xE11A3546:(hov?0xB71A293B:0x9B101D2F));g.fill(t.x(),74,t.x()+t.w(),76,sel?0xFF20D7C7:0x422C3E58);g.text(font,Component.translatable(t.category().key),t.x()+12,57,sel?0xFF20E5D2:0xFFC8D2E4,sel);}}
    private void drawMain(GuiGraphicsExtractor g,int mx,int my){
        g.fill(mainX,top,mainX+mainW,bottom,0xD30B1729);Section s=sections.isEmpty()?null:sections.get(0);
        if(s!=null){g.text(font,Component.translatable(s.title()),mainX+18,top+14,0xFFEAF3FF,true);g.text(font,Component.translatable(s.subtitle()),mainX+18,top+28,0xFF8492AA,false);}
        for(Row r:rows){int y=r.y()-(int)scroll;if(y+54<top+36||y>bottom)continue;drawRow(g,r,y,mx,my);}
    }
    private void drawRow(GuiGraphicsExtractor g,Row r,int y,int mx,int my){
        int x=mainX+14,w=mainW-28;boolean hov=mx>=x&&mx<=x+w&&my>=y&&my<=y+r.button().getHeight();
        g.fill(x,y,x+w,y+r.button().getHeight(),hov?0xD5182A3D:0xB7111F31);g.fill(x,y,x+2,y+r.button().getHeight(),hov?0xFF20D7C7:0x5A20D7C7);
        String title=Component.translatable(r.key()).getString();if(title.contains(": %s"))title=title.replace(": %s","");if(title.length()>54)title=title.substring(0,51)+"...";
        g.text(font,Component.literal(title),x+16,y+12,0xFFE8F0FF,true);
        if(r.kind()!=Kind.STATUS){String d=Component.translatable(r.desc()).getString();if(d.length()>76)d=d.substring(0,73)+"...";g.text(font,Component.literal(d),x+16,y+29,0xFF8491AA,false);}
        if(r.kind()==Kind.TOGGLE)drawToggle(g,x+w-84,y+16,"ВКЛ".equals(r.value().get().getString()));else{int vx=x+w-150;g.fill(vx,y+11,x+w-12,y+41,0x80152235);g.text(font,r.value().get(),x+w-94,y+22,r.kind()==Kind.STATUS?0xFF7EDEF0:0xFFC9D6EA,true);}
    }
    private void drawToggle(GuiGraphicsExtractor g,int x,int y,boolean on){g.fill(x,y,x+54,y+22,on?0xFF17BFAE:0xFF34465E);g.fill(x+2,y+2,x+52,y+20,on?0xFF16A99B:0xFF26364A);int cx=on?x+36:x+8;g.fill(cx,y+4,cx+14,y+18,0xFFF4FBFF);}
    private void drawSide(GuiGraphicsExtractor g,int mx,int my){
        g.fill(sideX,top,sideX+sideW,bottom,0xD30B1729);g.text(font,Component.translatable("superoptimizer.category.profiles"),sideX+16,top+14,0xFFEAF3FF,true);g.text(font,Component.literal("Выбор применяется сразу"),sideX+16,top+28,0xFF8492AA,false);
        for(Profile p:profiles){int x=p.button().getX(),y=p.y(),w=p.button().getWidth();boolean sel=p.preset()==activePreset,hov=mx>=x&&mx<=x+w&&my>=y&&my<=y+p.h();g.fill(x,y,x+w,y+p.h(),sel?0xE0173947:(hov?0xB3172A3E:0x98111F31));g.fill(x,y,x+2,y+p.h(),sel?0xFF20D7C7:0x3A314762);drawRadio(g,x+14,y+20,sel);g.text(font,profileName(p.preset()),x+38,y+13,0xFFE8F0FF,true);g.text(font,profileSub(p.preset()),x+38,y+31,0xFF91A0B9,false);if(sel)g.text(font,Component.literal("АКТИВЕН"),x+w-58,y+13,0xFF20D7C7,true);}
        int fy=profiles.get(profiles.size()-1).y()+76;String cur=activePreset==null?"Пользовательский":profileName(activePreset).getString();g.text(font,Component.literal("Текущий профиль"),sideX+16,fy,0xFF8492AA,false);g.text(font,Component.literal(cur),sideX+16,fy+13,0xFFEAF3FF,true);
        mini(g,sideX+14,fy+30,sideW-28,32,Component.literal("Сохранить как пользовательский"),mx,my);mini(g,sideX+14,fy+68,sideW-28,32,Component.literal("Сбросить в сбалансированный"),mx,my);
    }
    private void drawRadio(GuiGraphicsExtractor g,int x,int y,boolean on){g.fill(x,y,x+16,y+16,on?0xFF20D7C7:0xFF50627B);g.fill(x+3,y+3,x+13,y+13,0xFF0A1525);if(on)g.fill(x+5,y+5,x+11,y+11,0xFF20D7C7);}
    private void mini(GuiGraphicsExtractor g,int x,int y,int w,int h,Component c,int mx,int my){boolean hov=mx>=x&&mx<=x+w&&my>=y&&my<=y+h;g.fill(x,y,x+w,y+h,hov?0xD21C3A4B:0xAB15283D);g.fill(x,y+h-2,x+w,y+h,hov?0xFF20D7C7:0x5A30455E);g.text(font,c,x+12,y+11,hov?0xFFEFFFFF:0xFFC8D5E8,true);}
    private void drawHover(GuiGraphicsExtractor g,Row r){int x=sideX+14,y=bottom-112,w=sideW-28;g.fill(x,y,x+w,bottom-14,0xE0132237);g.fill(x,y,x+3,bottom-14,0xFF20D7C7);g.text(font,Component.literal("О настройке"),x+12,y+12,0xFF20D7C7,true);String d=Component.translatable(r.desc()).getString();if(d.length()>86)d=d.substring(0,83)+"...";g.text(font,Component.literal(d),x+12,y+31,0xFFB7C5D8,false);}
    private void drawFooter(GuiGraphicsExtractor g){g.fill(0,height-50,width,height,0xE30A1423);g.text(font,Component.literal("Наведи для подробностей • прокрутка работает по всему экрану"),mainX+18,height-31,0xFF7F8EA7,false);g.text(font,Component.translatable("superoptimizer.button.logs"),mainX+18,height-17,0xFFC9D6EA,true);int x=Math.max(mainX+mainW-156,mainX+150);g.fill(x,height-42,x+140,height-14,0xB7173947);g.text(font,Component.translatable("gui.done"),x+49,height-31,0xFFE8F4FF,true);}
    private Row hoveredRow(int mx,int my){for(Row r:rows){int y=r.y()-(int)scroll;Button b=r.button();if(mx>=b.getX()&&mx<=b.getX()+b.getWidth()&&my>=y&&my<=y+b.getHeight())return r;}return null;}
    @Override public void onClose(){close();}
    private void close(){config.save(minecraft.gameDirectory.toPath().resolve("config"));SuperOptimizerClient.applyConfig();minecraft.setScreenAndShow(parent);}
}