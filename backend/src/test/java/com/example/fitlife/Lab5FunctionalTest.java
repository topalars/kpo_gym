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
  var row=new LinkedHashMap<String,Object>();row.put("id",id);row.put("tech",technique);row.put("input",input);row.put("expected",expected);row.put("actual",actual);row.put("status",actual==expected?"Пройден":"Не пройден");results.add(row);
 }
 @Test void lab5Scenarios()throws Exception {
  setup();client("client@test.ru");
  check("EP-01","Эквивалентное разделение","POST /api/auth/login; client@test.ru; Testpass123!",200,()->call("POST","/api/auth/login",null,Map.of("email","client@test.ru","password","Testpass123!")));
  check("EP-02","Эквивалентное разделение","POST /api/auth/login; существующая почта; неверный пароль",401,()->call("POST","/api/auth/login",null,Map.of("email","client@test.ru","password","wrong")));
  check("EP-03","Эквивалентное разделение","POST /api/auth/login; неизвестная почта",401,()->call("POST","/api/auth/login",null,Map.of("email","none@test.ru","password","Testpass123!")));
  check("EP-04","Эквивалентное разделение","POST /api/auth/register; новая почта new@test.ru; корректные имя и пароль",200,()->call("POST","/api/auth/register",null,Map.of("name","Новый Клиент","email","new@test.ru","password","Testpass123!")));
  check("EP-05","Эквивалентное разделение","POST /api/auth/register; дубликат CLIENT@test.ru",409,()->call("POST","/api/auth/register",null,Map.of("name","Новый Клиент","email","CLIENT@test.ru","password","Testpass123!")));
  check("EP-06","Эквивалентное разделение","POST /api/auth/register; почта invalid-email",400,()->call("POST","/api/auth/register",null,Map.of("name","Новый Клиент","email","invalid-email","password","Testpass123!")));
  for(double w:new double[]{24.9,25,25.1,349.9,350,350.1}){setup();var c=client("client@test.ru");check("BV-W-"+w,"Граничные значения","PUT /api/progress; сегодня; вес="+w+"; жим=60",w<25||w>350?400:200,()->call("PUT","/api/progress",c,Map.of("date",LocalDate.now().toString(),"weight",w,"strength",60)));}
  for(int n:new int[]{7,8,9}){setup();check("BV-P-"+n,"Граничные значения","POST /api/auth/register; длина ASCII-пароля="+n,n<8?400:200,()->call("POST","/api/auth/register",null,Map.of("name","Новый Клиент","email","new@test.ru","password","A".repeat(n))));}
  for(int n:new int[]{79,80,81,249,250,251}){setup();var c=client("client@test.ru");check("BV-H-"+n,"Граничные значения","PATCH /api/me; корректное имя; рост="+n,n<80||n>250?400:200,()->call("PATCH","/api/me",c,Map.of("name","Клиент","phone","","height",n)));}
  setup();var coach=coach("coach@test.ru");var c=client("client@test.ru");var slot=createTraining(training("Силовая",coach,"10:00",1,"Зал1"));
  final var cs=c; final var sid=slot;
  check("CE-01","Причинно-следственный анализ","POST /api/bookings; клиент; будущее занятие; 1 место; нет пересечения",200,()->call("POST","/api/bookings",cs,Map.of("trainingId",sid)));
  var other=client("other@test.ru");check("CE-02","Причинно-следственный анализ","POST /api/bookings; другой клиент; последнее место занято",409,()->call("POST","/api/bookings",other,Map.of("trainingId",sid)));
  check("CE-03","Причинно-следственный анализ","DELETE /api/bookings/{trainingId}; существующая запись клиента",200,()->call("DELETE","/api/bookings/"+sid,cs,null));
  call("POST","/api/bookings",cs,Map.of("trainingId",sid));var coach2=coach("coach2@test.ru");var overlap=createTraining(training("Пересечение",coach2,"10:30",2,"Зал2"));
  check("CE-04","Причинно-следственный анализ","POST /api/bookings; есть место, но занятие 10:30 пересекается с 10:00–11:00",409,()->call("POST","/api/bookings",cs,Map.of("trainingId",overlap)));
  check("EG-01","Предугадывание ошибки","PUT /api/progress; невозможная дата 2026-02-30; вес=80; жим=60",400,()->call("PUT","/api/progress",cs,Map.of("date","2026-02-30","weight",80,"strength",60)));
  check("EG-02","Предугадывание ошибки","PUT /api/progress; отсутствует strength",400,()->call("PUT","/api/progress",cs,Map.of("date",LocalDate.now().toString(),"weight",80)));
  call("PUT","/api/progress",cs,Map.of("date",LocalDate.now().toString(),"weight",80,"strength",60));
  check("EG-03","Предугадывание ошибки","PUT /api/progress; повторная запись сегодня; вес=81; жим=65; остаётся одна запись",200,()->call("PUT","/api/progress",cs,Map.of("date",LocalDate.now().toString(),"weight",81,"strength",65)));
  var ps=snapshot(cs).get("progress");assertEquals(1,ps.size());assertEquals(81,ps.get(0).get("weight").asInt());
  var trainer=login("coach2@test.ru");check("EG-04","Предугадывание ошибки","PATCH /api/trainings/{id}; тренер изменяет чужое занятие",403,()->call("PATCH","/api/trainings/"+sid,trainer,training("Чужое",coach2,"14:00",2,"Зал3")));
  var late=createTraining(training("Вечерняя",coach,"18:00",3,"Зал1"));call("POST","/api/bookings",cs,Map.of("trainingId",late));
  check("EG-05","Предугадывание ошибки","PATCH /api/trainings/{id}; перенос занятого клиентом занятия на 10:30; другой тренер/зал",409,()->call("PATCH","/api/trainings/"+late,admin,training("Вечерняя",coach2,"10:30",3,"Зал3")));
  for(String role:List.of("guest","client","coach","admin"))for(boolean full:List.of(false,true)){
   setup();var co=coach("coach@test.ru");var id=createTraining(training("Силовая",co,"10:00",1,"Зал1"));
   if(full){var occupied=client("occupied@test.ru");status(200,call("POST","/api/bookings",occupied,Map.of("trainingId",id)));}
   MockHttpSession actor=switch(role){case "guest"->null;case "client"->client("client@test.ru");case "coach"->login("coach@test.ru");default->admin;};
   int expected=role.equals("guest")?401:!role.equals("client")?403:full?409:200;
   check("EX-"+role+"-"+(full?1:0),"Исчерпывающее тестирование","POST /api/bookings; роль="+role+"; свободных мест="+(full?0:1),expected,()->call("POST","/api/bookings",actor,Map.of("trainingId",id)));
  }
  var output=Path.of(System.getProperty("lab5.output", "target/lab5-results.json"));Files.createDirectories(output.getParent());json.writerWithDefaultPrettyPrinter().writeValue(output.toFile(),results);
  assertEquals(38,results.size());assertEquals(0,results.stream().filter(r->!r.get("status").equals("Пройден")).count());
 }
}

