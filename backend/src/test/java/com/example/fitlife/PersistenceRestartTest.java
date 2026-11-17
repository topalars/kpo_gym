package com.example.fitlife;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import com.example.fitlife.repository.UserAccountRepository;
import com.example.fitlife.service.FitLifeService;
import com.example.fitlife.dto.Requests.Register;
import static org.junit.jupiter.api.Assertions.*;

class PersistenceRestartTest {
 @Test void dataSurvivesApplicationRestart() throws Exception {
  try(var pg=EmbeddedPostgres.builder().setPort(0).start()){
   String[] args={"--server.port=0","--spring.datasource.url="+pg.getJdbcUrl("postgres","postgres"),"--spring.datasource.username=postgres","--spring.datasource.password=","--fitlife.admin-email=admin@test.ru","--fitlife.admin-password=Testpass123!"};
   String id;
   try(var first=new SpringApplicationBuilder(FitLifeApplication.class).run(args)){
    id=first.getBean(FitLifeService.class).register(new Register("Клиент","persist@test.ru","Testpass123!")).id;
   }
   try(var second=new SpringApplicationBuilder(FitLifeApplication.class).run(args)){
    var account=second.getBean(UserAccountRepository.class).findById(id).orElseThrow();assertEquals("persist@test.ru",account.email);
    assertEquals(2,second.getBean(UserAccountRepository.class).count());
   }
  }
 }
}
