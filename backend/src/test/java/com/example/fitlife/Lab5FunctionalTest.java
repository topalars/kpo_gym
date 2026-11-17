package com.example.fitlife;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.mock.web.MockHttpSession;
import java.util.*;
import java.util.concurrent.Callable;
import java.time.*;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

/** Black-box scenarios: requests and observable responses, without branch coverage criteria. */
class Lab5FunctionalTest extends ApiIntegrationTest {
 final List<Map<String,Object>> results=new ArrayList<>();
 void check(String id,String technique,String input,int expected,Callable<MvcResult> action)throws Exception{
  var r=action.call();int actual=r.getResponse().getStatus();
  var row=new LinkedHashMap<String,Object>();row.put("id",id);row.put("tech",technique);row.put("input",input);row.put("expected",expected);row.put("actual",actual);row.put("status",actual==expected?"РџСЂРѕР№РґРµРЅ":"РќРµ РїСЂРѕР№РґРµРЅ");results.add(row);
 }
 @Test void lab5Scenarios()throws Exception {
  setup();client("client@test.ru");
  check("EP-01","Р­РєРІРёРІР°Р»РµРЅС‚РЅРѕРµ СЂР°Р·РґРµР»РµРЅРёРµ","POST /api/auth/login; client@test.ru; Testpass123!",200,()->call("POST","/api/auth/login",null,Map.of("email","client@test.ru","password","Testpass123!")));
  check("EP-02","Р­РєРІРёРІР°Р»РµРЅС‚РЅРѕРµ СЂР°Р·РґРµР»РµРЅРёРµ","POST /api/auth/login; СЃСѓС‰РµСЃС‚РІСѓСЋС‰Р°СЏ РїРѕС‡С‚Р°; РЅРµРІРµСЂРЅС‹Р№ РїР°СЂРѕР»СЊ",401,()->call("POST","/api/auth/login",null,Map.of("email","client@test.ru","password","wrong")));
  check("EP-03","Р­РєРІРёРІР°Р»РµРЅС‚РЅРѕРµ СЂР°Р·РґРµР»РµРЅРёРµ","POST /api/auth/login; РЅРµРёР·РІРµСЃС‚РЅР°СЏ РїРѕС‡С‚Р°",401,()->call("POST","/api/auth/login",null,Map.of("email","none@test.ru","password","Testpass123!")));
  check("EP-04","Р­РєРІРёРІР°Р»РµРЅС‚РЅРѕРµ СЂР°Р·РґРµР»РµРЅРёРµ","POST /api/auth/register; РЅРѕРІР°СЏ РїРѕС‡С‚Р° new@test.ru; РєРѕСЂСЂРµРєС‚РЅС‹Рµ РёРјСЏ Рё РїР°СЂРѕР»СЊ",200,()->call("POST","/api/auth/register",null,Map.of("name","РќРѕРІС‹Р№ РљР»РёРµРЅС‚","email","new@test.ru","password","Testpass123!")));
  check("EP-05","Р­РєРІРёРІР°Р»РµРЅС‚РЅРѕРµ СЂР°Р·РґРµР»РµРЅРёРµ","POST /api/auth/register; РґСѓР±Р»РёРєР°С‚ CLIENT@test.ru",409,()->call("POST","/api/auth/register",null,Map.of("name","РќРѕРІС‹Р№ РљР»РёРµРЅС‚","email","CLIENT@test.ru","password","Testpass123!")));
  check("EP-06","Р­РєРІРёРІР°Р»РµРЅС‚РЅРѕРµ СЂР°Р·РґРµР»РµРЅРёРµ","POST /api/auth/register; РїРѕС‡С‚Р° invalid-email",400,()->call("POST","/api/auth/register",null,Map.of("name","РќРѕРІС‹Р№ РљР»РёРµРЅС‚","email","invalid-email","password","Testpass123!")));
  for(double w:new double[]{24.9,25,25.1,349.9,350,350.1}){setup();var c=client("client@test.ru");check("BV-W-"+w,"Р“СЂР°РЅРёС‡РЅС‹Рµ Р·РЅР°С‡РµРЅРёСЏ","PUT /api/progress; СЃРµРіРѕРґРЅСЏ; РІРµСЃ="+w+"; Р¶РёРј=60",w<25||w>350?400:200,()->call("PUT","/api/progress",c,Map.of("date",LocalDate.now().toString(),"weight",w,"strength",60)));}
  for(int n:new int[]{7,8,9}){setup();check("BV-P-"+n,"Р“СЂР°РЅРёС‡РЅС‹Рµ Р·РЅР°С‡РµРЅРёСЏ","POST /api/auth/register; РґР»РёРЅР° ASCII-РїР°СЂРѕР»СЏ="+n,n<8?400:200,()->call("POST","/api/auth/register",null,Map.of("name","РќРѕРІС‹Р№ РљР»РёРµРЅС‚","email","new@test.ru","password","A".repeat(n))));}
  for(int n:new int[]{79,80,81,249,250,251}){setup();var c=client("client@test.ru");check("BV-H-"+n,"Р“СЂР°РЅРёС‡РЅС‹Рµ Р·РЅР°С‡РµРЅРёСЏ","PATCH /api/me; РєРѕСЂСЂРµРєС‚РЅРѕРµ РёРјСЏ; СЂРѕСЃС‚="+n,n<80||n>250?400:200,()->call("PATCH","/api/me",c,Map.of("name","РљР»РёРµРЅС‚","phone","","height",n)));}
  setup();var coach=coach("coach@test.ru");var c=client("client@test.ru");var slot=createTraining(training("РЎРёР»РѕРІР°СЏ",coach,"10:00",1,"Р—Р°Р»1"));
  final var cs=c; final var sid=slot;
  check("CE-01","РџСЂРёС‡РёРЅРЅРѕ-СЃР»РµРґСЃС‚РІРµРЅРЅС‹Р№ Р°РЅР°Р»РёР·","POST /api/bookings; РєР»РёРµРЅС‚; Р±СѓРґСѓС‰РµРµ Р·Р°РЅСЏС‚РёРµ; 1 РјРµСЃС‚Рѕ; РЅРµС‚ РїРµСЂРµСЃРµС‡РµРЅРёСЏ",200,()->call("POST","/api/bookings",cs,Map.of("trainingId",sid)));
  var other=client("other@test.ru");check("CE-02","РџСЂРёС‡РёРЅРЅРѕ-СЃР»РµРґСЃС‚РІРµРЅРЅС‹Р№ Р°РЅР°Р»РёР·","POST /api/bookings; РґСЂСѓРіРѕР№ РєР»РёРµРЅС‚; РїРѕСЃР»РµРґРЅРµРµ РјРµСЃС‚Рѕ Р·Р°РЅСЏС‚Рѕ",409,()->call("POST","/api/bookings",other,Map.of("trainingId",sid)));
  check("CE-03","РџСЂРёС‡РёРЅРЅРѕ-СЃР»РµРґСЃС‚РІРµРЅРЅС‹Р№ Р°РЅР°Р»РёР·","DELETE /api/bookings/{trainingId}; СЃСѓС‰РµСЃС‚РІСѓСЋС‰Р°СЏ Р·Р°РїРёСЃСЊ РєР»РёРµРЅС‚Р°",200,()->call("DELETE","/api/bookings/"+sid,cs,null));
  call("POST","/api/bookings",cs,Map.of("trainingId",sid));var coach2=coach("coach2@test.ru");var overlap=createTraining(training("РџРµСЂРµСЃРµС‡РµРЅРёРµ",coach2,"10:30",2,"Р—Р°Р»2"));
  check("CE-04","РџСЂРёС‡РёРЅРЅРѕ-СЃР»РµРґСЃС‚РІРµРЅРЅС‹Р№ Р°РЅР°Р»РёР·","POST /api/bookings; РµСЃС‚СЊ РјРµСЃС‚Рѕ, РЅРѕ Р·Р°РЅСЏС‚РёРµ 10:30 РїРµСЂРµСЃРµРєР°РµС‚СЃСЏ СЃ 10:00вЂ“11:00",409,()->call("POST","/api/bookings",cs,Map.of("trainingId",overlap)));
  check("EG-01","РџСЂРµРґСѓРіР°РґС‹РІР°РЅРёРµ РѕС€РёР±РєРё","PUT /api/progress; РЅРµРІРѕР·РјРѕР¶РЅР°СЏ РґР°С‚Р° 2026-02-30; РІРµСЃ=80; Р¶РёРј=60",400,()->call("PUT","/api/progress",cs,Map.of("date","2026-02-30","weight",80,"strength",60)));
  check("EG-02","РџСЂРµРґСѓРіР°РґС‹РІР°РЅРёРµ РѕС€РёР±РєРё","PUT /api/progress; РѕС‚СЃСѓС‚СЃС‚РІСѓРµС‚ strength",400,()->call("PUT","/api/progress",cs,Map.of("date",LocalDate.now().toString(),"weight",80)));
  call("PUT","/api/progress",cs,Map.of("date",LocalDate.now().toString(),"weight",80,"strength",60));
  check("EG-03","РџСЂРµРґСѓРіР°РґС‹РІР°РЅРёРµ РѕС€РёР±РєРё","PUT /api/progress; РїРѕРІС‚РѕСЂРЅР°СЏ Р·Р°РїРёСЃСЊ СЃРµРіРѕРґРЅСЏ; РІРµСЃ=81; Р¶РёРј=65; РѕСЃС‚Р°С‘С‚СЃСЏ РѕРґРЅР° Р·Р°РїРёСЃСЊ",200,()->call("PUT","/api/progress",cs,Map.of("date",LocalDate.now().toString(),"weight",81,"strength",65)));
  var ps=snapshot(cs).get("progress");assertEquals(1,ps.size());assertEquals(81,ps.get(0).get("weight").asInt());
  var trainer=login("coach2@test.ru");check("EG-04","РџСЂРµРґСѓРіР°РґС‹РІР°РЅРёРµ РѕС€РёР±РєРё","PATCH /api/trainings/{id}; С‚СЂРµРЅРµСЂ РёР·РјРµРЅСЏРµС‚ С‡СѓР¶РѕРµ Р·Р°РЅСЏС‚РёРµ",403,()->call("PATCH","/api/trainings/"+sid,trainer,training("Р§СѓР¶РѕРµ",coach2,"14:00",2,"Р—Р°Р»3")));
  var late=createTraining(training("Р’РµС‡РµСЂРЅСЏСЏ",coach,"18:00",3,"Р—Р°Р»1"));call("POST","/api/bookings",cs,Map.of("trainingId",late));
  check("EG-05","РџСЂРµРґСѓРіР°РґС‹РІР°РЅРёРµ РѕС€РёР±РєРё","PATCH /api/trainings/{id}; РїРµСЂРµРЅРѕСЃ Р·Р°РЅСЏС‚РѕРіРѕ РєР»РёРµРЅС‚РѕРј Р·Р°РЅСЏС‚РёСЏ РЅР° 10:30; РґСЂСѓРіРѕР№ С‚СЂРµРЅРµСЂ/Р·Р°Р»",409,()->call("PATCH","/api/trainings/"+late,admin,training("Р’РµС‡РµСЂРЅСЏСЏ",coach2,"10:30",3,"Р—Р°Р»3")));
  for(String role:List.of("guest","client","coach","admin"))for(boolean full:List.of(false,true)){
   setup();var co=coach("coach@test.ru");var id=createTraining(training("РЎРёР»РѕРІР°СЏ",co,"10:00",1,"Р—Р°Р»1"));
   if(full){var occupied=client("occupied@test.ru");status(200,call("POST","/api/bookings",occupied,Map.of("trainingId",id)));}
   MockHttpSession actor=switch(role){case "guest"->null;case "client"->client("client@test.ru");case "coach"->login("coach@test.ru");default->admin;};
   int expected=role.equals("guest")?401:!role.equals("client")?403:full?409:200;
   check("EX-"+role+"-"+(full?1:0),"РСЃС‡РµСЂРїС‹РІР°СЋС‰РµРµ С‚РµСЃС‚РёСЂРѕРІР°РЅРёРµ","POST /api/bookings; СЂРѕР»СЊ="+role+"; СЃРІРѕР±РѕРґРЅС‹С… РјРµСЃС‚="+(full?0:1),expected,()->call("POST","/api/bookings",actor,Map.of("trainingId",id)));
  }
  var output=Path.of(System.getProperty("lab5.output", "target/lab5-results.json"));Files.createDirectories(output.getParent());json.writerWithDefaultPrettyPrinter().writeValue(output.toFile(),results);
  assertEquals(38,results.size());assertEquals(0,results.stream().filter(r->!r.get("status").equals("РџСЂРѕР№РґРµРЅ")).count());
 }
}

