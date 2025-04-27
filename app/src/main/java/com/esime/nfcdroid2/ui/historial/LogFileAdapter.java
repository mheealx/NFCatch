package com.esime.nfcdroid2.ui.historial;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.*;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;


public class LogFileAdapter extends RecyclerView.Adapter<LogFileAdapter.LogViewHolder> {

    private final List<File> files;
    private final Context context;

    public LogFileAdapter(Context context, List<File> files) {
        this.context = context;
        this.files = files;
    }

    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_2, parent, false);
        return new LogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        File file = files.get(position);
        holder.name.setText(file.getName());

        String date = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                .format(new Date(file.lastModified()));
        holder.date.setText(date);

        // Al hacer clic, abre el archivo seleccionado
        holder.itemView.setOnClickListener(v -> {
            Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", file);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "text/plain");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(Intent.createChooser(intent, "Abrir con..."));
        });
    }

    @Override
    public int getItemCount() {
        return files.size();
    }


    static class LogViewHolder extends RecyclerView.ViewHolder {
        TextView name, date;

        public LogViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(android.R.id.text1);
            date = itemView.findViewById(android.R.id.text2);
        }
    }
}
