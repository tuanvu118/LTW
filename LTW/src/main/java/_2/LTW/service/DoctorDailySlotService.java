package _2.LTW.service;

import _2.LTW.dto.response.DoctorDailySlotResponse.AvailableSlotReponse;
import _2.LTW.dto.response.DoctorDailySlotResponse.DoctorSlotResponse;
import _2.LTW.dto.response.DoctorDailySlotResponse.SlotAvailability;
import _2.LTW.entity.DoctorDailySlot.BookingSlotStatus;
import _2.LTW.entity.DoctorDailySlot.BookingType;
import _2.LTW.entity.DoctorDailySlot.DoctorDailySlot;
import _2.LTW.entity.DoctorWork.DoctorWork;
import _2.LTW.entity.DoctorWork.ShiftType;
import _2.LTW.entity.DoctorWork.SlotStatus;
import _2.LTW.exception.ErrorCode;
import _2.LTW.mapper.DoctorDailySlotMapper;
import _2.LTW.repository.CareServiceRepository;
import _2.LTW.repository.DoctorDailySlotRepository;
import _2.LTW.repository.DoctorWorkRepository;
import _2.LTW.repository.MedicalServiceRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DoctorDailySlotService {

    DoctorDailySlotRepository doctorDailySlotRepository;
    DoctorWorkRepository doctorWorkRepository;
    MedicalServiceRepository medicalServiceRepository;
    CareServiceRepository careServiceRepository;
    private final DoctorDailySlotMapper doctorDailySlotMapper;

    @Transactional
    public AvailableSlotReponse getAvailableSlots(
            LocalDate date,
            BookingType bookingType,
            List<Long> serviceIds
    ){

        generateSlotsIfNeeded(date);

        int duration = caculateTotalDuration(bookingType, serviceIds);

        int requiredSlots = (int) Math.ceil(duration / 30.0);

        List<Object[]> rows = doctorDailySlotRepository.findAvailableSlots(date);

        Map<Long, List<LocalTime>> doctorSlots = new HashMap<>();

        Map<Long, DoctorSlotResponse> doctorMap = new HashMap<>();

        for(Object[] row : rows){
            LocalTime time = (LocalTime) row[0];
            Long doctorId = (Long) row[1];
            String doctorName = (String) row[2];
            String imageUrl = (String) row[3];

            doctorMap.putIfAbsent(
                    doctorId,
                    DoctorSlotResponse.builder()
                            .doctorId(doctorId)
                            .doctorName(doctorName)
                            .imageUrl(imageUrl)
                            .build()
            );

            doctorSlots.computeIfAbsent(doctorId, k -> new ArrayList<>()).add(time);
        }

        Map<LocalTime, List<Long>> timeDoctorMap = new LinkedHashMap<>();

        for(Map.Entry<Long, List<LocalTime>> entry : doctorSlots.entrySet()){
            Long doctorId = entry.getKey();
            List<LocalTime> times = entry.getValue();

            Collections.sort(times);

            int consecutive = 1;

            if(requiredSlots == 1){
                timeDoctorMap.computeIfAbsent(times.getFirst(), k -> new ArrayList<>()).add(doctorId);
            }

            for(int i = 1; i < times.size(); i++){
                if(times.get(i).equals(times.get(i - 1).plusMinutes(30))){
                    consecutive++;
                }
                else{
                    consecutive = 1;
                }
                if(consecutive >= requiredSlots){

                    LocalTime startTime = times.get(i - requiredSlots + 1);

                    timeDoctorMap.computeIfAbsent(startTime, k -> new ArrayList<>()).add(doctorId);
                }
            }

        }

        List<LocalTime> times = new ArrayList<>(timeDoctorMap.keySet());
        Collections.sort(times);

        List<DoctorSlotResponse> doctors = new ArrayList<>(doctorMap.values());

        List<SlotAvailability> availabilities = timeDoctorMap.entrySet().stream()
                .map(e -> SlotAvailability.builder()
                        .time(e.getKey())
                        .doctorIds(e.getValue())
                        .build()
                ).toList();

        return AvailableSlotReponse.builder()
                .times(times)
                .doctors(doctors)
                .availabilities(availabilities)
                .build();

    }

    public void generateSlotsIfNeeded(LocalDate date){

        boolean exists = doctorDailySlotRepository.existsDoctorDailySlotBySlotDate(date);

        if(exists){
            return;
        }

        Integer dayOfWeek = date.getDayOfWeek().getValue();

        List<DoctorWork> doctorWorks = doctorWorkRepository
                .findDoctorWorkByDayOfWeekAndSlotStatus(dayOfWeek, SlotStatus.APPROVED);

        List<DoctorDailySlot> slots = new ArrayList<>();

        for(DoctorWork ws : doctorWorks){
            LocalTime start = getShiftStart(ws.getShiftType());
            LocalTime end = getShiftEnd(ws.getShiftType());

            LocalTime time = start;

            while(time.isBefore(end)){
                DoctorDailySlot slot = DoctorDailySlot.builder()
                        .doctor(ws.getDoctor())
                        .slotTime(time)
                        .slotDate(date)
                        .shiftType(ws.getShiftType())
                        .status(BookingSlotStatus.AVAILABLE)
                        .build();

                slots.add(slot);

                time = time.plusMinutes(30);
            }
        }

        try {
            doctorDailySlotRepository.saveAll(slots);
        }
        catch (DataIntegrityViolationException e){
            throw ErrorCode.BAD_REQUEST.toException("Slot đã được tạo trước đó rồi");
        }

    }


    public void reservedSlots(
            Long doctorId,
            LocalDate date,
            LocalTime time,
            int duration,
            BookingType bookingType,
            Long bookingId
    ){

        int requiredSlots = calulateRequiredSlots(duration);

        List<DoctorDailySlot> slots = doctorDailySlotRepository.lockSlots(
                doctorId,
                date,
                time,
                time.plusMinutes(duration)
        );

        if(slots.size() < requiredSlots){
            throw ErrorCode.CONFLICT.toException("Không đủ thời gian khả dụng trong ca làm việc");
        }

        List<DoctorDailySlot> reserved = slots.stream()
                .limit(requiredSlots)
                .toList();

        boolean unavailable = reserved.stream()
                .anyMatch(s -> s.getStatus() != BookingSlotStatus.AVAILABLE);

        if(unavailable){
            throw ErrorCode.CONFLICT.toException("Bác sĩ đã có lịch tại thời điểm này");
        }

        reserved.forEach(s -> {
            s.setStatus(BookingSlotStatus.RESERVED);
            s.setBookingType(bookingType);
            s.setBookingId(bookingId);
        });

    }

    public void releaseSlots(
            Long doctorId,
            LocalDate date,
            LocalTime time,
            int duration
    ){

        int requiredSlots = calulateRequiredSlots(duration);

        List<DoctorDailySlot> slots = doctorDailySlotRepository.findSlotsByDoctorIdAndDateTime(
                doctorId,
                date,
                time,
                time.plusMinutes(duration)
        );

        slots.stream().limit(requiredSlots)
                .forEach(s -> {
                    s.setStatus(BookingSlotStatus.AVAILABLE);
                    s.setBookingType(null);
                    s.setBookingId(null);
                });

    }

    private LocalTime getShiftStart(ShiftType shiftType){

        return switch (shiftType){
            case MORNING -> LocalTime.of(8, 0);
            case AFTERNOON -> LocalTime.of(13, 0);
        };

    }

    private LocalTime getShiftEnd(ShiftType shift) {

        return switch (shift) {
            case MORNING -> LocalTime.of(12, 0);
            case AFTERNOON -> LocalTime.of(17, 0);
        };
    }

    public int caculateTotalDuration(
            BookingType bookingType,
            List<Long> serviceIds
    ){

        if (serviceIds == null || serviceIds.isEmpty()) {
            throw ErrorCode.BAD_REQUEST.toException("Bạn chưa chọn dịch vụ nào");
        }

        if(bookingType == BookingType.MEDICAL){
            return medicalServiceRepository.sumDuration(serviceIds);
        }

        return careServiceRepository.sumDuration(serviceIds);

    }

    public int calulateRequiredSlots(int duration){

        return (int) Math.ceil(duration / 30.0);

    }

}
