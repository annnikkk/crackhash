package org.example.worker.Controller;

import jakarta.annotation.PostConstruct;
import org.example.worker.Model.WorkerRequest;
import org.example.worker.Service.CrackService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.UnknownHostException;

@RestController
@RequestMapping("/internal")
public class WorkerController {

    private final CrackService service;

    public WorkerController(CrackService service) {
        this.service = service;
    }

    @PostConstruct
    public void init(){
        Thread t =  new Thread(() -> {
            try {
                service.startCracking();
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
        });
        t.setDaemon(true);
        t.start();
    }
}
