import { THU_LABEL } from "./format";

function overlapPeriod(first, second) {
  const start = Math.max(first.tietBatDau, second.tietBatDau);
  const end = Math.min(
    first.tietBatDau + first.soTiet - 1,
    second.tietBatDau + second.soTiet - 1
  );
  return start <= end ? { start, end } : null;
}

export function findPlanConflicts(plannedClasses, registeredClasses = []) {
  const conflicts = [];
  const pairs = [
    ...plannedClasses.flatMap((planned, index) =>
      plannedClasses.slice(index + 1).map((other) => [planned, other, "Giỏ dự kiến"])
    ),
    ...plannedClasses.flatMap((planned) =>
      registeredClasses.map((registered) => [planned, registered, "Đã đăng ký"])
    ),
  ];

  pairs.forEach(([first, second, secondSource]) => {
    if (first.monHocId === second.monHocId) {
      conflicts.push({
        key: `subject-${first.id}-${second.id}`,
        type: "subject",
        first,
        second,
        secondSource,
      });
    }

    const periods = first.thu === second.thu ? overlapPeriod(first, second) : null;
    if (periods) {
      conflicts.push({
        key: `schedule-${first.id}-${second.id}`,
        type: "schedule",
        first,
        second,
        secondSource,
        day: THU_LABEL[first.thu] || `Thứ ${first.thu}`,
        ...periods,
      });
    }
  });

  return conflicts;
}
