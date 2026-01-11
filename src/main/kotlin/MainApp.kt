import java.awt.*
import java.sql.*
import javax.swing.*
import javax.swing.table.DefaultTableModel

class MainApp {
    private val connection: Connection by lazy { connectToDatabase() }
    private lateinit var frame: JFrame
    private lateinit var table: JTable
    private lateinit var model: DefaultTableModel
    private lateinit var idField: JTextField
    private lateinit var namaField: JTextField
    private lateinit var kelasField: JTextField
    private lateinit var tanggalField: JTextField
    private lateinit var statusField: JComboBox<String>

    init {
        initializeDatabase()
        createUI()
        loadData()
    }

    private fun connectToDatabase(): Connection {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver")
            return DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/absensi_siswa",
                "root", // Ganti dengan username MySQL Anda
                ""      // Ganti dengan password MySQL Anda jika ada
            )
        } catch (e: Exception) {
            JOptionPane.showMessageDialog(null, "Gagal terhubung ke database: ${e.message}")
            throw RuntimeException(e)
        }
    }

    private fun initializeDatabase() {
        val statement = connection.createStatement()
        statement.executeUpdate("""
            CREATE TABLE IF NOT EXISTS absensi (
                id INT AUTO_INCREMENT PRIMARY KEY,
                nama VARCHAR(100) NOT NULL,
                kelas VARCHAR(50) NOT NULL,
                tanggal DATE NOT NULL,
                status ENUM('Hadir', 'Izin', 'Sakit', 'Alfa') NOT NULL
            )
        """.trimIndent())
        statement.close()
    }

    private fun createUI() {
        frame = JFrame("Aplikasi Sistem Absensi Siswa")
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.layout = BorderLayout()

        // Panel input form
        val inputPanel = JPanel(GridBagLayout())
        inputPanel.border = BorderFactory.createTitledBorder("Form Input Absensi")
        val gbc = GridBagConstraints()
        gbc.insets = Insets(5, 5, 5, 5)

        gbc.gridx = 0; gbc.gridy = 0; inputPanel.add(JLabel("ID:"), gbc)
        idField = JTextField(10)
        idField.isEditable = false
        gbc.gridx = 1; inputPanel.add(idField, gbc)

        gbc.gridx = 0; gbc.gridy = 1; inputPanel.add(JLabel("Nama Siswa:"), gbc)
        namaField = JTextField(20)
        gbc.gridx = 1; inputPanel.add(namaField, gbc)

        gbc.gridx = 0; gbc.gridy = 2; inputPanel.add(JLabel("Kelas:"), gbc)
        kelasField = JTextField(20)
        gbc.gridx = 1; inputPanel.add(kelasField, gbc)

        gbc.gridx = 0; gbc.gridy = 3; inputPanel.add(JLabel("Tanggal (YYYY-MM-DD):"), gbc)
        tanggalField = JTextField(20)
        gbc.gridx = 1; inputPanel.add(tanggalField, gbc)

        gbc.gridx = 0; gbc.gridy = 4; inputPanel.add(JLabel("Status:"), gbc)
        statusField = JComboBox(arrayOf("Hadir", "Izin", "Sakit", "Alfa"))
        gbc.gridx = 1; inputPanel.add(statusField, gbc)

        frame.add(inputPanel, BorderLayout.NORTH)

        // Panel tombol aksi
        val buttonPanel = JPanel(FlowLayout())

        val addButton = JButton("Tambah")
        addButton.addActionListener {
            addRecord()
        }
        buttonPanel.add(addButton)

        val updateButton = JButton("Ubah")
        updateButton.addActionListener {
            updateRecord()
        }
        buttonPanel.add(updateButton)

        val deleteButton = JButton("Hapus")
        deleteButton.addActionListener {
            deleteRecord()
        }
        buttonPanel.add(deleteButton)

        val resetButton = JButton("Reset")
        resetButton.addActionListener {
            resetForm()
        }
        buttonPanel.add(resetButton)

        frame.add(buttonPanel, BorderLayout.SOUTH)

        // Tabel data
        val columnNames = arrayOf("ID", "Nama Siswa", "Kelas", "Tanggal", "Status")
        model = DefaultTableModel(columnNames, 0)
        table = JTable(model)

        // Event listener saat baris dipilih di tabel
        table.selectionModel.addListSelectionListener { e ->
            if (!e.valueIsAdjusting && table.selectedRow >= 0) {
                val row = table.selectedRow
                idField.text = table.getValueAt(row, 0).toString()
                namaField.text = table.getValueAt(row, 1).toString()
                kelasField.text = table.getValueAt(row, 2).toString()
                tanggalField.text = table.getValueAt(row, 3).toString()
                statusField.selectedItem = table.getValueAt(row, 4).toString()
            }
        }

        val scrollPane = JScrollPane(table)
        frame.add(scrollPane, BorderLayout.CENTER)

        frame.size = Dimension(800, 600)
        frame.setLocationRelativeTo(null)
        frame.isVisible = true
    }

    private fun loadData() {
        model.rowCount = 0 // Clear existing data
        try {
            val statement = connection.createStatement()
            val resultSet = statement.executeQuery("SELECT * FROM absensi ORDER BY tanggal DESC")

            while (resultSet.next()) {
                val row = arrayOf(
                    resultSet.getInt("id"),
                    resultSet.getString("nama"),
                    resultSet.getString("kelas"),
                    resultSet.getDate("tanggal"),
                    resultSet.getString("status")
                )
                model.addRow(row)
            }

            resultSet.close()
            statement.close()
        } catch (e: SQLException) {
            JOptionPane.showMessageDialog(frame, "Error loading data: ${e.message}")
        }
    }

    private fun addRecord() {
        if (validateInput()) {
            try {
                val sql = """
                    INSERT INTO absensi (nama, kelas, tanggal, status) 
                    VALUES (?, ?, ?, ?)
                """.trimIndent()

                val preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
                preparedStatement.setString(1, namaField.text.trim())
                preparedStatement.setString(2, kelasField.text.trim())
                preparedStatement.setDate(3, Date.valueOf(tanggalField.text.trim()))
                preparedStatement.setString(4, statusField.selectedItem as String)

                preparedStatement.executeUpdate()

                val generatedKeys = preparedStatement.generatedKeys
                if (generatedKeys.next()) {
                    val newId = generatedKeys.getInt(1)
                    val newRow = arrayOf(
                        newId,
                        namaField.text.trim(),
                        kelasField.text.trim(),
                        tanggalField.text.trim(),
                        statusField.selectedItem as String
                    )
                    model.addRow(newRow)

                    JOptionPane.showMessageDialog(frame, "Data berhasil ditambahkan!")
                    resetForm()
                }

                preparedStatement.close()
            } catch (e: SQLException) {
                JOptionPane.showMessageDialog(frame, "Error adding record: ${e.message}")
            }
        }
    }

    private fun updateRecord() {
        if (table.selectedRow >= 0) {
            val selectedId = (table.getValueAt(table.selectedRow, 0) as Number).toInt()

            if (validateInput()) {
                try {
                    val sql = """
                        UPDATE absensi SET nama=?, kelas=?, tanggal=?, status=? WHERE id=?
                    """.trimIndent()

                    val preparedStatement = connection.prepareStatement(sql)
                    preparedStatement.setString(1, namaField.text.trim())
                    preparedStatement.setString(2, kelasField.text.trim())
                    preparedStatement.setDate(3, Date.valueOf(tanggalField.text.trim()))
                    preparedStatement.setString(4, statusField.selectedItem as String)
                    preparedStatement.setInt(5, selectedId)

                    if (preparedStatement.executeUpdate() > 0) {
                        // Update the table model
                        model.setValueAt(namaField.text.trim(), table.selectedRow, 1)
                        model.setValueAt(kelasField.text.trim(), table.selectedRow, 2)
                        model.setValueAt(tanggalField.text.trim(), table.selectedRow, 3)
                        model.setValueAt(statusField.selectedItem as String, table.selectedRow, 4)

                        JOptionPane.showMessageDialog(frame, "Data berhasil diperbarui!")
                        resetForm()
                    } else {
                        JOptionPane.showMessageDialog(frame, "Gagal memperbarui data.")
                    }

                    preparedStatement.close()
                } catch (e: SQLException) {
                    JOptionPane.showMessageDialog(frame, "Error updating record: ${e.message}")
                }
            }
        } else {
            JOptionPane.showMessageDialog(frame, "Pilih data yang ingin diubah!")
        }
    }

    private fun deleteRecord() {
        if (table.selectedRow >= 0) {
            val selectedId = (table.getValueAt(table.selectedRow, 0) as Number).toInt()

            val result = JOptionPane.showConfirmDialog(
                frame,
                "Apakah Anda yakin ingin menghapus data ini?",
                "Konfirmasi Hapus",
                JOptionPane.YES_NO_OPTION
            )

            if (result == JOptionPane.YES_OPTION) {
                try {
                    val sql = "DELETE FROM absensi WHERE id=?"
                    val preparedStatement = connection.prepareStatement(sql)
                    preparedStatement.setInt(1, selectedId)

                    if (preparedStatement.executeUpdate() > 0) {
                        model.removeRow(table.selectedRow)
                        JOptionPane.showMessageDialog(frame, "Data berhasil dihapus!")
                        resetForm()
                    } else {
                        JOptionPane.showMessageDialog(frame, "Gagal menghapus data.")
                    }

                    preparedStatement.close()
                } catch (e: SQLException) {
                    JOptionPane.showMessageDialog(frame, "Error deleting record: ${e.message}")
                }
            }
        } else {
            JOptionPane.showMessageDialog(frame, "Pilih data yang ingin dihapus!")
        }
    }

    private fun resetForm() {
        idField.text = ""
        namaField.text = ""
        kelasField.text = ""
        tanggalField.text = ""
        statusField.selectedIndex = 0
        table.clearSelection()
    }

    private fun validateInput(): Boolean {
        if (namaField.text.trim().isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Nama siswa harus diisi!")
            namaField.requestFocus()
            return false
        }

        if (kelasField.text.trim().isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Kelas harus diisi!")
            kelasField.requestFocus()
            return false
        }

        if (tanggalField.text.trim().isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Tanggal harus diisi! (Format: YYYY-MM-DD)")
            tanggalField.requestFocus()
            return false
        }

        // Validasi format tanggal
        try {
            Date.valueOf(tanggalField.text.trim())
        } catch (e: IllegalArgumentException) {
            JOptionPane.showMessageDialog(frame, "Format tanggal tidak valid! Gunakan format YYYY-MM-DD")
            tanggalField.requestFocus()
            return false
        }

        return true
    }
}

fun main() {
    SwingUtilities.invokeLater {
        // Menggunakan look and feel sistem
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
        } catch (e: Exception) {
            e.printStackTrace()
        }
        MainApp()
    }
}