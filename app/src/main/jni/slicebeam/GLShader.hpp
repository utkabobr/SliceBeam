///|/ Copyright (c) Prusa Research 2017 - 2022 Enrico Turri @enricoturri1966, Vojtěch Bubník @bubnikv, Lukáš Matěna @lukasmatena
///|/
///|/ ported from lib/Slic3r/GUI/GLShader.pm:
///|/ Copyright (c) Prusa Research 2016 - 2017 Vojtěch Bubník @bubnikv
///|/
///|/ PrusaSlicer is released under the terms of the AGPLv3 or higher
///|/
#ifndef SLICEBEAM_GUI_GLSHADER_HPP
#define SLICEBEAM_GUI_GLSHADER_HPP

#include <array>
#include <string>
#include <string_view>

#include "libslic3r/Point.hpp"

namespace Slic3r {

class ColorRGB;
class ColorRGBA;

class GLShaderProgram
{
public:
    enum class EShaderType
    {
        Vertex,
        Fragment,
        Count
    };

    typedef std::array<std::string, static_cast<size_t>(EShaderType::Count)> ShaderFilenames;
    typedef std::array<std::string, static_cast<size_t>(EShaderType::Count)> ShaderSources;

private:
    std::string m_name;
    unsigned int m_id{ 0 };
    std::vector<std::pair<std::string, int>> m_attrib_location_cache;
    std::vector<std::pair<std::string, int>> m_uniform_location_cache;

public:
    ~GLShaderProgram();

    bool init_from_texts(const std::string& name, const ShaderSources& sources);

    const std::string& get_name() const { return m_name; }
    unsigned int get_id() const { return m_id; }

    void start_using() const;
    void stop_using() const;

    void set_uniform(const char* name, int value) const { set_uniform(get_uniform_location(name), value); }
    void set_uniform(const char* name, bool value) const { set_uniform(get_uniform_location(name), value); }
    void set_uniform(const char* name, float value) const { set_uniform(get_uniform_location(name), value); }
    void set_uniform(const char* name, double value) const { set_uniform(get_uniform_location(name), value); }
    void set_uniform(const char* name, const std::array<int, 2>& value) const { set_uniform(get_uniform_location(name), value); }
    void set_uniform(const char* name, const std::array<int, 3>& value) const { set_uniform(get_uniform_location(name), value); }
    void set_uniform(const char* name, const std::array<int, 4>& value) const { set_uniform(get_uniform_location(name), value); }
    void set_uniform(const char* name, const std::array<float, 2>& value) const { set_uniform(get_uniform_location(name), value); }
    void set_uniform(const char* name, const std::array<float, 3>& value) const { set_uniform(get_uniform_location(name), value); }
    void set_uniform(const char* name, const std::array<float, 4>& value) const { set_uniform(get_uniform_location(name), value); }
    void set_uniform(const char* name, const std::array<double, 4>& value) const { set_uniform(get_uniform_location(name), value); }
    void set_uniform(const char* name, const float* value, size_t size) const { set_uniform(get_uniform_location(name), value, size); }
    void set_uniform(const char* name, const Transform3f& value) const { set_uniform(get_uniform_location(name), value); }
    void set_uniform(const char* name, const Transform3d& value) const { set_uniform(get_uniform_location(name), value); }
    void set_uniform(const char* name, const Matrix3f& value) const { set_uniform(get_uniform_location(name), value); }
    void set_uniform(const char* name, const Matrix3d& value) const { set_uniform(get_uniform_location(name), value); }
    void set_uniform(const char* name, const Matrix4f& value) const { set_uniform(get_uniform_location(name), value); }
    void set_uniform(const char* name, const Matrix4d& value) const { set_uniform(get_uniform_location(name), value); }
    void set_uniform(const char* name, const Vec2f& value) const { set_uniform(get_uniform_location(name), value); }
    void set_uniform(const char* name, const Vec2d& value) const { set_uniform(get_uniform_location(name), value); }
    void set_uniform(const char* name, const Vec3f& value) const { set_uniform(get_uniform_location(name), value); }
    void set_uniform(const char* name, const Vec3d& value) const { set_uniform(get_uniform_location(name), value); }
    void set_uniform(const char* name, const ColorRGB& value) const { set_uniform(get_uniform_location(name), value); }
    void set_uniform(const char* name, const ColorRGBA& value) const { set_uniform(get_uniform_location(name), value); }

    void set_uniform(int id, int value) const;
    void set_uniform(int id, bool value) const;
    void set_uniform(int id, float value) const;
    void set_uniform(int id, double value) const;
    void set_uniform(int id, const std::array<int, 2>& value) const;
    void set_uniform(int id, const std::array<int, 3>& value) const;
    void set_uniform(int id, const std::array<int, 4>& value) const;
    void set_uniform(int id, const std::array<float, 2>& value) const;
    void set_uniform(int id, const std::array<float, 3>& value) const;
    void set_uniform(int id, const std::array<float, 4>& value) const;
    void set_uniform(int id, const std::array<double, 4>& value) const;
    void set_uniform(int id, const float* value, size_t size) const;
    void set_uniform(int id, const Transform3f& value) const;
    void set_uniform(int id, const Transform3d& value) const;
    void set_uniform(int id, const Matrix3f& value) const;
    void set_uniform(int id, const Matrix3d& value) const;
    void set_uniform(int id, const Matrix4f& value) const;
    void set_uniform(int id, const Matrix4d& value) const;
    void set_uniform(int id, const Vec2f& value) const;
    void set_uniform(int id, const Vec2d& value) const;
    void set_uniform(int id, const Vec3f& value) const;
    void set_uniform(int id, const Vec3d& value) const;
    void set_uniform(int id, const ColorRGB& value) const;
    void set_uniform(int id, const ColorRGBA& value) const;

    // returns -1 if not found
    int get_attrib_location(const char* name) const;
    // returns -1 if not found
    int get_uniform_location(const char* name) const;
};

} // namespace Slic3r

#endif /* SLICEBEAM_GUI_GLSHADER_HPP */
