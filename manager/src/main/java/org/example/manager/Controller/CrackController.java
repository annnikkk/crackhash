package org.example.manager.Controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.manager.Model.*;
import org.example.manager.NotFoundException;
import org.example.manager.Service.CrackService;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/hash")
public class CrackController {

    private final CrackService crackService;

    @PostMapping("/crack")
    public CrackResponse crackHash(@RequestBody CrackRequest request) {

        String requestId = crackService.createRequest(
                request.getHash(),
                request.getMaxLen()
        );

        return new CrackResponse(requestId);
    }

    @GetMapping("/status/{requestId}")
    public RequestStatus getStatus(@PathVariable String requestId) {
        RequestStatus status = crackService.getStatus(requestId);
        if (status == null) {
            throw new NotFoundException(requestId);
        }
        return status;
    }

    @PostMapping("/crack/complete")
    public void completeCrack(@RequestBody CrackCompleteRequest body) {
        log.info("Complete from worker: requestId={}, data={}", body.getRequestId(), body.getData());
        crackService.completeRequest(body.getRequestId(), body.getData());
    }

    @GetMapping("/internal/task")
    public WorkerRequest getTask() {
        WorkerRequest task = crackService.getTask();

        if (task != null) {
            log.info("Task выдаётся: from={} to={}", task.getFrom(), task.getTo());
        } else {
            log.info("Задач больше нет");
        }

        return task;
    }
}