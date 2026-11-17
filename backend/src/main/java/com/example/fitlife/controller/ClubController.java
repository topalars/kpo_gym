package com.example.fitlife.controller;
import com.example.fitlife.service.FitLifeService;
import com.example.fitlife.dto.Requests.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController @RequestMapping("/api")
public class ClubController {
 private final FitLifeService service;
 public ClubController(FitLifeService service){this.service=service;}
 public record BookingInput(@NotBlank String trainingId) {}
 @GetMapping("/snapshot") public Map<String,Object> snapshot(){return service.snapshot();}
 @GetMapping("/me") public Map<String,Object> me(){return service.userView(service.me());}
 @PatchMapping("/me") public void profile(@Valid @RequestBody Profile r){service.updateProfile(r);}
 @GetMapping("/trainings") public Object trainings(){return service.snapshot().get("slots");}
 @PostMapping("/trainings") public void createTraining(@Valid @RequestBody Training r){service.saveTraining(r,null);}
 @PatchMapping("/trainings/{id}") public void training(@PathVariable String id,@Valid @RequestBody Training r){service.saveTraining(r,id);}
 @DeleteMapping("/trainings/{id}") public void deleteTraining(@PathVariable String id){service.deleteTraining(id);}
 @PostMapping("/bookings") public void book(@Valid @RequestBody BookingInput r){service.book(r.trainingId(),false);}
 @DeleteMapping("/bookings/{trainingId}") public void cancel(@PathVariable String trainingId){service.book(trainingId,true);}
 @GetMapping("/progress") public Object progress(){return service.snapshot().get("progress");}
 @PutMapping("/progress") public void progress(@Valid @RequestBody Progress r){service.saveProgress(r);}
 @GetMapping("/clients") public Object clients(){return ((java.util.List<?>)service.snapshot().get("users")).stream().filter(x->((Map<?,?>)x).get("role").equals("ROLE_USER")).toList();}
 @PatchMapping("/clients/{id}") public void member(@PathVariable String id,@Valid @RequestBody Member r){service.saveMember(r,id);}
 @GetMapping("/trainers") public Object trainers(){return ((java.util.List<?>)service.snapshot().get("users")).stream().filter(x->((Map<?,?>)x).get("role").equals("ROLE_COACH")).toList();}
 @PostMapping("/trainers") public void trainer(@Valid @RequestBody Trainer r){service.saveTrainer(r,null);}
 @PatchMapping("/trainers/{id}") public void trainer(@PathVariable String id,@Valid @RequestBody Trainer r){service.saveTrainer(r,id);}
 @GetMapping("/memberships") public Object memberships(){return service.userView(service.me());}
 @PutMapping("/memberships/{clientId}") public void membership(@PathVariable String clientId,@Valid @RequestBody Plan r){service.savePlan(r,clientId);}
 @GetMapping("/events") public Object events(){return service.snapshot().get("events");}
 @PostMapping("/events") public void event(@Valid @RequestBody Event r){service.saveEvent(r,null);}
 @PatchMapping("/events/{id}") public void event(@PathVariable String id,@Valid @RequestBody Event r){service.saveEvent(r,id);}
 @DeleteMapping("/events/{id}") public void deleteEvent(@PathVariable String id){service.deleteEvent(id);}
 @PostMapping("/events/{id}/registration") public void join(@PathVariable String id){service.joinEvent(id,false);}
 @DeleteMapping("/events/{id}/registration") public void leave(@PathVariable String id){service.joinEvent(id,true);}
 @GetMapping("/notifications") public Object notifications(){return service.ownNotifications();}
 @PatchMapping("/notifications/{id}/read") public void read(@PathVariable String id){service.readNotification(id);}
 @GetMapping("/statistics") public Object statistics(){return service.statistics();}
}
