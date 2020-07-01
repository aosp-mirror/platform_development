import JSZip from 'jszip';

export default {
  name: 'SaveAsZip',
  methods: {
    saveAs(blob, filename) {
      const a = document.createElement("a");
      a.style = "display: none";
      document.body.appendChild(a);

      const url = window.URL.createObjectURL(blob);

      a.href = url;
      a.download = filename;
      a.click();
      window.URL.revokeObjectURL(url);

      document.body.removeChild(a);
    },
    async downloadAsZip(files) {
      const zip = new JSZip();

      for (const file of files) {
        const blob = await fetch(file.blobUrl).then(r => r.blob());
        zip.file(file.filename, blob);
      }

      const zipFile = await zip.generateAsync({ type: 'blob' });

      this.saveAs(zipFile, "winscope.zip");
    }
  }
}