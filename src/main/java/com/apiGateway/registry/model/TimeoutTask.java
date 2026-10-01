package com.apiGateway.registry.model;


import lombok.*;


@Getter
@Setter

public class TimeoutTask {

   private String instanceID ;
   private String serviceName;
   private int remainingRounds ;
   public boolean isCancelled ;



      public TimeoutTask(String serviceName,String instanceID, int remainingRounds ) {
        this.instanceID = instanceID;
        this.remainingRounds = remainingRounds;
          this.serviceName = serviceName;

    }





}
