package com.package2.publisher.controller;

import com.package2.publisher.model.EventData;
import com.package2.publisher.model.PublisherModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.util.HashMap;
import static java.lang.Thread.sleep;

@Component
@RestController
public class PublisherController {
    @Value("${brokerIp}")
    private String brokerIp;

    @Value("${ec2Port}")
    private String ec2Port;

    private HashMap<String, PublisherModel> publisherObjectHelperMap = new HashMap<>();

    RestTemplate restTemplate = new RestTemplate();

    // Method for new publisher to get added @Shreya Krishnamoorthy
    @PostMapping(value = "/addPublisher")
    public String addPublisher(@RequestBody String publisherId) {
        PublisherModel publisherModel = new PublisherModel();
        publisherModel.setPublisherId(publisherId);
        publisherModel.setStatus("active");
        if (!publisherObjectHelperMap.containsKey(publisherId)) {
            publisherObjectHelperMap.put(publisherId, publisherModel);
        } else {
            System.out.println("Publisher with ID: " + publisherId + " already exists");
        }
        String brokerUrl = "http://" + brokerIp + ":" + ec2Port;
        String url = brokerUrl + "/addPublisher";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<PublisherModel> req = new HttpEntity<>(publisherModel, headers);
        try {
            ResponseEntity<HttpStatus> responseEntity = restTemplate.exchange(url,
                    HttpMethod.POST, req, HttpStatus.class);

            HttpStatusCode httpStatus = responseEntity.getStatusCode();
            System.out
                    .println("Received response from broker with status code for publisher add: " + httpStatus.value());
            String responseString = "Received response from broker with status code: "
                    + httpStatus.value()
                    + "\nPublisher with ID: " + publisherId + " is added to the broker system";
            return responseString;
        } catch (Exception e) {
            System.out.println("An error occurred while communicating with the broker: " + e.getMessage());
            throw new RuntimeException("An error occurred while communicating with the broker", e);
        }
    }

    // Method for publisher to change status between "active" and "inactive" @Shreya
    // Krishnamoorthy
    @PostMapping(value = "/changePublisherStatus")
    public String changePublisherStatus(@RequestBody String publisherId) {
        if (!publisherObjectHelperMap.containsKey(publisherId)) {
            System.out.println("Publisher with ID " + publisherId + " not found");
            return "Publisher not found";
        }
        PublisherModel publisherModel = publisherObjectHelperMap.get(publisherId);
        String status = publisherModel.getStatus();
        if (status.equals("active")) {
            publisherModel.setStatus("inactive");
        } else if (status.equals("inactive")) {
            publisherModel.setStatus("active");
        } else {
            System.out.println("Error in setting Publisher Status for publisher ID:" + publisherId);
            return "Error in setting publisher status";
        }

        String brokerUrl = "http://" + brokerIp + ":" + ec2Port;
        String url = brokerUrl + "/changePublisherStatus";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<PublisherModel> req = new HttpEntity<>(publisherModel, headers);
        try {
            ResponseEntity<HttpStatus> responseEntity = restTemplate.exchange(url,
                    HttpMethod.POST, req, HttpStatus.class);

            HttpStatusCode httpStatus = responseEntity.getStatusCode();
            System.out.println("Received response from broker with status code for publisher status change: "
                    + httpStatus.value());
            String responseString = "Received response from broker with status code: "
                    + httpStatus.value()
                    + "\nPublisher with ID: " + publisherId + " changed status to " + publisherModel.getStatus();
            return responseString;
        } catch (Exception e) {
            System.out.println("An error occurred while communicating with the broker: " + e.getMessage());
            throw new RuntimeException("An error occurred while communicating with the broker", e);
        }
    }

    // Method to push event data to the broker @Shreya Krishnamoorthy
    @PostMapping(value = "/pushEvent")
    public String pushEvent(@RequestBody EventData eventData) {

        String brokerUrl = "http://" + brokerIp + ":" + ec2Port;
        String url = brokerUrl + "/pushEvent";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<EventData> req = new HttpEntity<>(eventData, headers);
        try {
            ResponseEntity<HttpStatus> responseEntity = restTemplate.exchange(url,
                    HttpMethod.POST, req, HttpStatus.class);

            HttpStatusCode httpStatus = responseEntity.getStatusCode();
            System.out.println("Received response from broker with status code: " + httpStatus.value());
            return String.valueOf(Integer.parseInt(String.valueOf(httpStatus.value())));
        } catch (Exception e) {
            System.out.println("An error occurred while communicating with the broker: " + e.getMessage());
            throw new RuntimeException("An error occurred while communicating with the broker", e);
        }
    }

    // Method to access an input file with event data and create eventData Object
    // and call pushEvent using jobs at regular intervals
    @GetMapping("/startEventGeneration")
    public void generateAndPushEventData() {
        String filePath = "events.csv";
        Resource resource = new ClassPathResource(filePath);
        try (InputStream inputStream = resource.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] eventDataArray = line.split(",");
                if (eventDataArray.length == 5) { // Each line has 5 fields: eventId, occasion, publisherId,
                                                  // eventLocation, message
                    int eventId = Integer.parseInt(eventDataArray[0]);
                    String occasion = eventDataArray[1];
                    String publisherId = eventDataArray[2];
                    String eventLocation = eventDataArray[3];
                    String message = eventDataArray[4];

                    EventData eventData = new EventData();
                    eventData.setEventId(eventId);
                    eventData.setOccasion(occasion);
                    eventData.setPublisherId(publisherId);
                    eventData.setEventLocation(eventLocation);
                    eventData.setMessage(message);

                    int responseCodeInt = 0; // initial value
                    while (responseCodeInt != 200) // in other cases except success, keep retrying
                    {
                        System.out.println("Pushing event with ID: " + eventId);
                        String responseCode = pushEvent(eventData);
                        responseCodeInt = Integer.parseInt(responseCode);
                    }
                    System.out.println(
                            "Event with event ID:" + eventId + " pushed to broker by Publisher ID:" + publisherId);
                    sleep(30000); // Sleeps for 30 secs
                } else {
                    System.out.println("Invalid line in the event data file: " + line);
                }
            }
        } catch (IOException e) {
            System.out.println("An error occurred while reading the event data file: " + e.getMessage());
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
