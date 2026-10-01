package com.apiGateway.registry.service;



import com.apiGateway.registry.model.ServiceInstance;
import com.apiGateway.registry.model.TimeoutTask;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.concurrent.ConcurrentLinkedQueue;


@Component
public class InstanceEviction {


    @Autowired
    @Lazy
    private RegisterService registerService;

    private volatile int currentTick = 0;

    public ConcurrentLinkedQueue<TimeoutTask>[] wheel = new ConcurrentLinkedQueue[60];



    @PostConstruct
    public void initializer ()
    {
        for (int i = 0; i < wheel.length; i++) {
            wheel[i] = new ConcurrentLinkedQueue<>();
        }

    }




    // future-cart calculation on Gateway ping
    public void scheduleTimeout (ServiceInstance serviceInst , String instanceId, int delaySeconds) {



        int ticksToMove = delaySeconds;

        int newCartIndex = (currentTick + ticksToMove) % 60;

        int rounds = ticksToMove / 60 ;


        TimeoutTask newTask = new TimeoutTask(serviceInst.getServiceName(), instanceId, rounds);

        wheel[newCartIndex].add(newTask);


        serviceInst.setTimeoutReference(newTask);


    }






    @Scheduled(fixedDelay = 1000)
    public void advanceWheel () {

        currentTick = (currentTick + 1) % 60;

          ConcurrentLinkedQueue<TimeoutTask> cart = wheel[currentTick];


          for (TimeoutTask task : cart) // run the loop for each TimeoutTask object that is within the cart
          {
              if (task.isCancelled) {
                  cart.remove(task); // Throw it in the trash
                  continue;
              }
              if (task.getRemainingRounds() > 0) {
                  task.setRemainingRounds(task.getRemainingRounds() - 1); // Subtract a round, leave it in the cart
                  continue;
              }

              // It's not canceled, and rounds == 0.
              // EXECUTE THE EVICTION!
              registerService.deleteInstance(task.getServiceName(), task.getInstanceID());
              cart.remove(task); // Remove from wheel

          }


      }





}




