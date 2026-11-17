package com.example.fitlife.service;

import com.example.fitlife.entity.*;
import com.example.fitlife.repository.*;
import com.example.fitlife.dto.Requests.*;
import com.example.fitlife.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.jdbc.core.JdbcTemplate;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FitLifeService {
 private final UserAccountRepository users;
 private final ClientProfileRepository clients;
 private final TrainerProfileRepository trainers;
 private final MembershipRepository memberships;
 private final TrainingSessionRepository sessions;
 private final BookingRepository bookings;
 private final ProgressRecordRepository progress;
 private final ClubEventRepository events;
 private final EventRegistrationRepository registrations;
 private final NotificationRepository notifications;
 private final PasswordEncoder passwords;
 private final JdbcTemplate jdbc;
 public FitLifeService(UserAccountRepository users,ClientProfileRepository clients,TrainerProfileRepository trainers,
   MembershipRepository memberships,TrainingSessionRepository sessions,BookingRepository bookings,
   ProgressRecordRepository progress,ClubEventRepository events,EventRegistrationRepository registrations,
   NotificationRepository notifications,PasswordEncoder passwords,JdbcTemplate jdbc){
  this.users=users;this.clients=clients;this.trainers=trainers;this.memberships=memberships;this.sessions=sessions;
  this.bookings=bookings;this.progress=progress;this.events=events;this.registrations=registrations;
  this.notifications=notifications;this.passwords=passwords;this.jdbc=jdbc;
 }
 // A single PostgreSQL row serializes mutations across application instances. It protects
 // capacity and cross-session time conflicts in the same transaction. Adequate for a small club.
 private void lock(){jdbc.queryForObject("SELECT id FROM mutation_lock WHERE id=1 FOR UPDATE",Integer.class);}
 private ApiException bad(String m){return new ApiException(400,m);}
 private ApiException conflict(String m){return new ApiException(409,m);}
 public boolean admin(UserAccount u){return Set.of("ROLE_ADMIN","ROLE_SUPERADMIN").contains(u.role);}
 public UserAccount me(){
  var a=SecurityContextHolder.getContext().getAuthentication();
  if(a==null||!a.isAuthenticated())throw new ApiException(401,"Войдите в аккаунт.");
  var user=users.findById(a.getName()).orElseThrow(()->new ApiException(401,"Войдите в аккаунт."));
  if(!user.active)throw new ApiException(401,"Аккаунт приостановлен.");
  return user;
 }
 private UserAccount role(String... roles){var u=me();if(Arrays.stream(roles).noneMatch(r->r.equals(u.role)))throw new ApiException(403,"Недостаточно прав.");return u;}
 public UserAccount authenticate(Login r){
  var u=users.findByEmailIgnoreCase(r.email().trim()).orElse(null);
  if(u==null||!u.active||!passwords.matches(r.password(),u.passwordHash))throw new ApiException(401,"Неверная почта или пароль.");return u;
 }
 private void checkPassword(String p){if(p==null||p.length()<8||p.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72)throw bad("Пароль: не менее 8 символов и не более 72 байт UTF-8.");}
 private UserAccount account(String name,String email,String password,String role){
  checkPassword(password);if(name.trim().length()<2)throw bad("Введите имя из двух и более символов.");
  if(users.findByEmailIgnoreCase(email.trim()).isPresent())throw conflict("Эта почта уже зарегистрирована.");
  var u=new UserAccount();u.name=name.trim();u.email=email.trim().toLowerCase(Locale.ROOT);u.passwordHash=passwords.encode(password);u.role=role;u.active=true;u.phone="";return users.save(u);
 }
 @Transactional public UserAccount register(Register r){
  lock();var u=account(r.name(),r.email(),r.password(),"ROLE_USER");var p=new ClientProfile();p.userId=u.id;clients.save(p);
  var m=new Membership();m.userId=u.id;m.plan="Без абонемента";memberships.save(m);notify(u.id,"Добро пожаловать в FitLife! Выберите тренировку в расписании.");return u;
 }
 @Transactional public void bootstrap(String email,String password){
  lock();if(users.findAll().stream().anyMatch(this::admin))return;
  if(email==null||!email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")||password==null||password.isBlank())throw new IllegalStateException("Для первого запуска задайте ADMIN_EMAIL и ADMIN_PASSWORD.");
  account("Администратор FitLife",email,password,"ROLE_ADMIN");
 }
 @Transactional(readOnly=true) public Map<String,Object> userView(UserAccount u){
  var v=new LinkedHashMap<String,Object>();v.put("id",u.id);v.put("name",u.name);v.put("email",u.email);v.put("role",u.role);v.put("phone",u.phone==null?"":u.phone);v.put("active",u.active);
  clients.findAll().stream().filter(p->p.userId.equals(u.id)).findFirst().ifPresent(p->v.put("height",p.height));
  trainers.findAll().stream().filter(p->p.userId.equals(u.id)).findFirst().ifPresent(p->v.put("specialty",p.specialty));
  memberships.findAll().stream().filter(p->p.userId.equals(u.id)).findFirst().ifPresent(p->{v.put("plan",p.plan);v.put("planUntil",p.planUntil);});return v;
 }
 @Transactional(readOnly=true) public Map<String,Object> snapshot(){
  var u=me();boolean a=admin(u),coach=u.role.equals("ROLE_COACH");
  var bs=bookings.findAll();var ss=sessions.findAll().stream().filter(s->!coach||s.coachId.equals(u.id)).toList();
  var slotIds=ss.stream().map(s->s.id).collect(Collectors.toSet());
  var myClients=bs.stream().filter(b->slotIds.contains(b.trainingId)).map(b->b.clientId).collect(Collectors.toSet());
  var us=users.findAll().stream().filter(p->a||p.id.equals(u.id)||p.role.equals("ROLE_COACH")||(coach&&myClients.contains(p.id))).map(this::userView).toList();
  var sv=ss.stream().map(s->{var v=new LinkedHashMap<String,Object>();v.put("id",s.id);v.put("title",s.title);v.put("coachId",s.coachId);v.put("date",s.date);v.put("time",s.time.toString());v.put("duration",s.duration);v.put("capacity",s.capacity);v.put("room",s.room);v.put("members",bs.stream().filter(b->b.trainingId.equals(s.id)).map(b->a||coach||b.clientId.equals(u.id)?b.clientId:"occupied").toList());return v;}).toList();
  var regs=registrations.findAll();var ev=events.findAll().stream().filter(e->a||e.published).map(e->{var v=new LinkedHashMap<String,Object>();v.put("id",e.id);v.put("title",e.title);v.put("description",e.description);v.put("date",e.date);v.put("time",e.time.toString());v.put("type",e.type);v.put("capacity",e.capacity);v.put("published",e.published);v.put("members",regs.stream().filter(r->r.eventId.equals(e.id)).map(r->a||r.clientId.equals(u.id)?r.clientId:"occupied").toList());return v;}).toList();
  return Map.of("user",userView(u),"users",us,"slots",sv,"progress",progress.findAll().stream().filter(p->p.userId.equals(u.id)).toList(),"events",ev,"notifications",ownNotifications());
 }
 @Transactional public void updateProfile(Profile r){
  lock();var u=me();if(r.name().trim().length()<2)throw bad("Введите имя.");u.name=r.name().trim();u.phone=r.phone();users.save(u);
  if(u.role.equals("ROLE_USER")){var p=clients.findAll().stream().filter(x->x.userId.equals(u.id)).findFirst().orElseThrow();p.height=r.height();clients.save(p);}
 }
 @Transactional public void saveTrainer(Trainer r,String id){
  lock();role("ROLE_ADMIN","ROLE_SUPERADMIN");
  UserAccount u;
  if(id==null)u=account(r.name(),r.email(),r.password(),"ROLE_COACH");
  else {u=users.findById(id).orElseThrow(()->new ApiException(404,"Тренер не найден."));if(!u.role.equals("ROLE_COACH"))throw bad("Пользователь не является тренером.");
   if(users.findByEmailIgnoreCase(r.email().trim()).filter(p->!p.id.equals(id)).isPresent())throw conflict("Почта уже занята.");
   u.name=r.name().trim();u.email=r.email().trim().toLowerCase(Locale.ROOT);
   if(r.password()!=null&&!r.password().isBlank()){checkPassword(r.password());u.passwordHash=passwords.encode(r.password());}
  }
  if(r.name().trim().length()<2)throw bad("Введите имя.");u.phone=r.phone();u.active=r.active();users.save(u);
  var p=trainers.findAll().stream().filter(x->x.userId.equals(u.id)).findFirst().orElseGet(TrainerProfile::new);p.userId=u.id;p.specialty=r.specialty().trim();trainers.save(p);
 }
 @Transactional public void saveMember(Member r,String id){
  lock();role("ROLE_ADMIN","ROLE_SUPERADMIN");var u=client(id);if(r.name().trim().length()<2)throw bad("Введите имя.");
  savePlan(new Plan(r.plan(),r.planUntil()),id);
  u.name=r.name().trim();u.phone=r.phone();u.active=r.active();users.save(u);
 }
 private UserAccount client(String id){var u=users.findById(id).orElseThrow(()->new ApiException(404,"Клиент не найден."));if(!u.role.equals("ROLE_USER"))throw bad("Ожидался клиент.");return u;}
 @Transactional public void savePlan(Plan r,String id){
  lock();role("ROLE_ADMIN","ROLE_SUPERADMIN");client(id);
  if(!Set.of("Без абонемента","Дневной","Безлимит","Персональный").contains(r.plan())||(!r.plan().equals("Без абонемента")&&r.planUntil()==null))throw bad("Проверьте абонемент и срок действия.");
  var m=memberships.findAll().stream().filter(p->p.userId.equals(id)).findFirst().orElseGet(Membership::new);m.userId=id;m.plan=r.plan();m.planUntil=r.plan().equals("Без абонемента")?null:r.planUntil();memberships.save(m);notify(id,"Абонемент обновлён: "+m.plan+(m.planUntil==null?"":" до "+m.planUntil));
 }
 private boolean overlap(TrainingSession a,TrainingSession b){var a0=LocalDateTime.of(a.date,a.time);var b0=LocalDateTime.of(b.date,b.time);return a0.isBefore(b0.plusMinutes(b.duration))&&b0.isBefore(a0.plusMinutes(a.duration));}
 private void future(LocalDate date,LocalTime time){if(!LocalDateTime.of(date,time).isAfter(LocalDateTime.now()))throw bad("Выберите дату и время в будущем.");}
 private TrainingSession training(String id){return sessions.findById(id).orElseThrow(()->new ApiException(404,"Тренировка не найдена."));}
 private void manage(UserAccount u,TrainingSession s){if(!admin(u)&&!s.coachId.equals(u.id))throw new ApiException(403,"Можно менять только свои тренировки.");}
 @Transactional public void saveTraining(Training r,String id){
  lock();var u=role("ROLE_COACH","ROLE_ADMIN","ROLE_SUPERADMIN");
  var s=id==null?new TrainingSession():training(id);if(id!=null)manage(u,s);
  s.coachId=admin(u)?r.coachId():u.id;
  var coach=users.findById(s.coachId==null?"":s.coachId).orElseThrow(()->bad("Выберите тренера."));
  if(!coach.active||!coach.role.equals("ROLE_COACH"))throw bad("Выберите активного тренера.");
  future(r.date(),r.time());if(r.title().trim().isBlank()||r.room().trim().isBlank())throw bad("Укажите название и зал.");
  s.title=r.title().trim();s.date=r.date();s.time=r.time();s.duration=r.duration();s.capacity=r.capacity();s.room=r.room().trim();
  var bs=bookings.findAll();var participants=bs.stream().filter(b->b.trainingId.equals(s.id)).map(b->b.clientId).collect(Collectors.toSet());
  if(participants.size()>s.capacity)throw conflict("Мест меньше, чем записанных клиентов.");
  for(var other:sessions.findAll())if(!other.id.equals(s.id)&&overlap(s,other)){
   if(other.coachId.equals(s.coachId))throw conflict("У тренера уже есть занятие в это время.");
   if(other.room.equalsIgnoreCase(s.room))throw conflict("Зал занят в это время.");
   if(bs.stream().anyMatch(b->b.trainingId.equals(other.id)&&participants.contains(b.clientId)))throw conflict("У участника будет пересечение тренировок.");
  }
  sessions.save(s);participants.forEach(p->notify(p,"Тренировка обновлена: "+s.title+" "+s.date+" "+s.time));notify(s.coachId,"Расписание обновлено: "+s.title+" "+s.date+" "+s.time);
 }
 @Transactional public void deleteTraining(String id){
  lock();var u=role("ROLE_COACH","ROLE_ADMIN","ROLE_SUPERADMIN");var s=training(id);manage(u,s);
  bookings.findAll().stream().filter(b->b.trainingId.equals(id)).forEach(b->notify(b.clientId,"Тренировка отменена: "+s.title+" "+s.date+" "+s.time));
  notify(s.coachId,"Тренировка отменена: "+s.title);sessions.delete(s);sessions.flush();
 }
 @Transactional public void book(String id,boolean cancel){
  lock();var u=role("ROLE_USER");var s=training(id);future(s.date,s.time);
  var bs=bookings.findAll();var existing=bs.stream().filter(b->b.trainingId.equals(id)&&b.clientId.equals(u.id)).findFirst();
  if(cancel){if(existing.isEmpty())throw new ApiException(404,"Запись не найдена.");bookings.delete(existing.get());}
  else {
   if(existing.isPresent())throw conflict("Вы уже записаны.");
   if(!users.findById(s.coachId).orElseThrow().active)throw conflict("Запись к этому тренеру приостановлена.");
   if(bs.stream().filter(b->b.trainingId.equals(id)).count()>=s.capacity)throw conflict("Свободных мест больше нет.");
   var bookedIds=bs.stream().filter(b->b.clientId.equals(u.id)).map(b->b.trainingId).collect(Collectors.toSet());
   if(sessions.findAll().stream().anyMatch(o->bookedIds.contains(o.id)&&overlap(s,o)))throw conflict("У вас уже есть тренировка в это время.");
   var b=new Booking();b.trainingId=id;b.clientId=u.id;bookings.save(b);
  }
  notify(u.id,(cancel?"Запись отменена: ":"Вы записаны: ")+s.title+" "+s.date+" "+s.time);
  notify(s.coachId,(cancel?"Клиент отменил запись: ":"Новая запись: ")+u.name+", "+s.title);
 }
 @Transactional public void saveProgress(Progress r){
  lock();var u=role("ROLE_USER");var p=progress.findAll().stream().filter(x->x.userId.equals(u.id)&&x.date.equals(r.date())).findFirst().orElseGet(ProgressRecord::new);
  p.userId=u.id;p.date=r.date();p.weight=r.weight();p.strength=r.strength();progress.save(p);
 }
 @Transactional public void saveEvent(Event r,String id){
  lock();role("ROLE_ADMIN","ROLE_SUPERADMIN");future(r.date(),r.time());
  var e=id==null?new ClubEvent():events.findById(id).orElseThrow(()->new ApiException(404,"Событие не найдено."));
  if(registrations.findAll().stream().filter(x->x.eventId.equals(e.id)).count()>r.capacity())throw conflict("Мест меньше, чем участников.");
  e.title=r.title().trim();e.description=r.description().trim();e.date=r.date();e.time=r.time();e.type=r.type();e.capacity=r.capacity();e.published=r.published();events.save(e);
  registrations.findAll().stream().filter(x->x.eventId.equals(e.id)).forEach(x->notify(x.clientId,"Событие обновлено: "+e.title+" "+e.date+" "+e.time));
 }
 @Transactional public void deleteEvent(String id){
  lock();role("ROLE_ADMIN","ROLE_SUPERADMIN");var e=events.findById(id).orElseThrow(()->new ApiException(404,"Событие не найдено."));
  registrations.findAll().stream().filter(x->x.eventId.equals(id)).forEach(x->notify(x.clientId,"Событие отменено: "+e.title));events.delete(e);events.flush();
 }
 @Transactional public void joinEvent(String id,boolean cancel){
  lock();var u=role("ROLE_USER");var e=events.findById(id).orElseThrow(()->new ApiException(404,"Событие не найдено."));
  var rs=registrations.findAll();var own=rs.stream().filter(r->r.eventId.equals(id)&&r.clientId.equals(u.id)).findFirst();
  if(cancel){if(own.isEmpty())throw new ApiException(404,"Участие не найдено.");registrations.delete(own.get());}
  else {if(!e.published)throw new ApiException(404,"Событие недоступно.");future(e.date,e.time);
   if(own.isPresent())throw conflict("Вы уже участвуете.");if(rs.stream().filter(r->r.eventId.equals(id)).count()>=e.capacity)throw conflict("Свободных мест больше нет.");
   var r=new EventRegistration();r.clientId=u.id;r.eventId=id;registrations.save(r);
  }
  notify(u.id,(cancel?"Участие отменено: ":"Вы участвуете: ")+e.title);
 }
 private void notify(String userId,String message){var n=new Notification();n.userId=userId;n.message=message;n.createdAt=LocalDateTime.now();n.read=false;notifications.save(n);}
 @Transactional(readOnly=true) public List<Notification> ownNotifications(){var u=me();return notifications.findAll().stream().filter(n->n.userId.equals(u.id)).sorted(Comparator.comparing((Notification n)->n.createdAt).reversed()).limit(100).toList();}
 @Transactional public void readNotification(String id){lock();var u=me();var n=notifications.findById(id).orElseThrow(()->new ApiException(404,"Уведомление не найдено."));if(!n.userId.equals(u.id))throw new ApiException(403,"Нет доступа.");n.read=true;notifications.save(n);}
 @Transactional(readOnly=true) public Map<String,Object> statistics(){role("ROLE_ADMIN","ROLE_SUPERADMIN");return Map.of("clients",users.findAll().stream().filter(u->u.role.equals("ROLE_USER")).count(),"trainers",trainers.count(),"trainings",sessions.count(),"bookings",bookings.count(),"events",events.count());}
}
