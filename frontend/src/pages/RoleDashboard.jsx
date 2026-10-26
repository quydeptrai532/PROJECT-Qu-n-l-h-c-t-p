import { useAuth } from "../context/AuthContext";
import StudentDashboard from "./student/StudentDashboard";
import LecturerDashboard from "./lecturer/LecturerDashboard";

export default function RoleDashboard() {
  const { user } = useAuth();
  if (user?.role === "GIANG_VIEN") {
    return <LecturerDashboard />;
  }
  return <StudentDashboard />;
}
