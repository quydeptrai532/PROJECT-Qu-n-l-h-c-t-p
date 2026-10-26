import { Navigate, Route, Routes } from "react-router-dom";
import ProtectedRoute from "./components/ProtectedRoute";
import { homePath, useAuth } from "./context/AuthContext";
import LoginPage from "./pages/LoginPage";
import ProfilePage from "./pages/ProfilePage";
import RoleDashboard from "./pages/RoleDashboard";
import RegisterPage from "./pages/student/RegisterPage";
import MyCoursesPage from "./pages/student/MyCoursesPage";
import TimetablePage from "./pages/student/TimetablePage";
import GradesPage from "./pages/student/GradesPage";
import LecturerClassesPage from "./pages/lecturer/LecturerClassesPage";
import LecturerTimetablePage from "./pages/lecturer/TimetablePage";
import GradeEntryPage from "./pages/lecturer/GradeEntryPage";
import DashboardPage from "./pages/admin/DashboardPage";
import SemesterPage from "./pages/admin/SemesterPage";
import FacultyPage from "./pages/admin/FacultyPage";
import MajorPage from "./pages/admin/MajorPage";
import AdminClassPage from "./pages/admin/AdminClassPage";
import SubjectPage from "./pages/admin/SubjectPage";
import CourseClassPage from "./pages/admin/CourseClassPage";
import StudentPage from "./pages/admin/StudentPage";
import LecturerPage from "./pages/admin/LecturerPage";

function RootRedirect() {
  const { user } = useAuth();
  return <Navigate to={user ? homePath(user.role) : "/login"} replace />;
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/" element={<RootRedirect />} />

      <Route
        path="/tong-quan"
        element={
          <ProtectedRoute roles={["SINH_VIEN", "GIANG_VIEN"]}>
            <RoleDashboard />
          </ProtectedRoute>
        }
      />

      <Route
        path="/dang-ky"
        element={
          <ProtectedRoute roles={["SINH_VIEN"]}>
            <RegisterPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/hoc-phan-cua-toi"
        element={
          <ProtectedRoute roles={["SINH_VIEN"]}>
            <MyCoursesPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/thoi-khoa-bieu"
        element={
          <ProtectedRoute roles={["SINH_VIEN"]}>
            <TimetablePage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/diem"
        element={
          <ProtectedRoute roles={["SINH_VIEN"]}>
            <GradesPage />
          </ProtectedRoute>
        }
      />

      <Route
        path="/lop-cua-toi"
        element={
          <ProtectedRoute roles={["GIANG_VIEN"]}>
            <LecturerClassesPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/lop-cua-toi/thoi-khoa-bieu"
        element={
          <ProtectedRoute roles={["GIANG_VIEN"]}>
            <LecturerTimetablePage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/lop-cua-toi/:id/nhap-diem"
        element={
          <ProtectedRoute roles={["GIANG_VIEN"]}>
            <GradeEntryPage />
          </ProtectedRoute>
        }
      />

      <Route
        path="/quan-tri"
        element={
          <ProtectedRoute roles={["QUAN_TRI"]}>
            <DashboardPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/quan-tri/hoc-ky"
        element={
          <ProtectedRoute roles={["QUAN_TRI"]}>
            <SemesterPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/quan-tri/khoa"
        element={
          <ProtectedRoute roles={["QUAN_TRI"]}>
            <FacultyPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/quan-tri/nganh"
        element={
          <ProtectedRoute roles={["QUAN_TRI"]}>
            <MajorPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/quan-tri/lop-hanh-chinh"
        element={
          <ProtectedRoute roles={["QUAN_TRI"]}>
            <AdminClassPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/quan-tri/mon-hoc"
        element={
          <ProtectedRoute roles={["QUAN_TRI"]}>
            <SubjectPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/quan-tri/lop-hoc-phan"
        element={
          <ProtectedRoute roles={["QUAN_TRI"]}>
            <CourseClassPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/quan-tri/sinh-vien"
        element={
          <ProtectedRoute roles={["QUAN_TRI"]}>
            <StudentPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/quan-tri/giang-vien"
        element={
          <ProtectedRoute roles={["QUAN_TRI"]}>
            <LecturerPage />
          </ProtectedRoute>
        }
      />

      <Route
        path="/ho-so"
        element={
          <ProtectedRoute>
            <ProfilePage />
          </ProtectedRoute>
        }
      />

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
